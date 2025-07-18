package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.mapper.CommunitymanagerMapper;
import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityLevelEnum;
import com.swj.shiwujie.model.enums.community.CommunityTypeEnum;
import com.swj.shiwujie.model.enums.community.IsDefaultCommunityEnum;
import com.swj.shiwujie.model.request.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.CommunityVolunteerRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.InnerVolunteerService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author Administrator
* @description 针对表【Community(社区信息表)】的数据库操作Service实现
* @createDate 2025-07-18 14:44:37
*/
@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper, Community>
    implements CommunityService{


    @DubboReference
    private InnerVolunteerService innerVolunteerService;

    @Resource
    private CommunitymanagerMapper communitymanagerMapper;


    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    @Override
    public CommunityLoginSuccessVO communityRegister(CommunityRegisterRequest communityRegisterRequest) {


        //1- 判断社区注册人手机号格式是否正确,手机号是否存在
        CommunityVolunteerRegisterRequest requestVolunteer = communityRegisterRequest.getVolunteer();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(requestVolunteer),ErrorCode.PARAMS_ERROR,"信息填写不全");
        String phone = requestVolunteer.getPhone();
        Volunteer volunteer = innerVolunteerService.getByPhone(phone);
        if(ObjUtil.isNull(volunteer)){
            //  - 不存在
            //    - 自动注册志愿者账号且实名,身份证是否合法
            volunteer = new Volunteer();
            BeanUtils.copyProperties(requestVolunteer,volunteer);
            boolean save = innerVolunteerService.save(volunteer);
        }
        //  - 存在
        //    - 检查是否实名,没实名自动实名,身份证是否合法
        String idCard = requestVolunteer.getIdCard();
        if(IdcardUtil.isValidCard(idCard)){
            volunteer.setIdCard(SecureUtil.md5(idCard));
        }


        //2- 检测是否有对应省份的上级账号
        // 检查省
        String province = communityRegisterRequest.getProvince();
        Community provinceCommunity = this.getByName(province);
        if(ObjUtil.isNull(provinceCommunity)){
            //  - 不存在
            //    - 自动创建上级社区
            Community community = new Community();
            community.setProvince(province);
            community.setCommunityLevelId(CommunityLevelEnum.PROVINCE.getLevelId());
            community.setIsDefaultCommunity(IsDefaultCommunityEnum.TRUE.getIsDefaultCommunity());
            community.setCommunityName(province);
            community.setCommunityStatus(1);
            boolean save = this.save(community);
            ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR);
        }
        // 检查市
        String city = communityRegisterRequest.getCity();
        Community cityCommunity = this.getByName(city);
        if(ObjUtil.isNull(cityCommunity)){
            Community community = new Community();
            community.setProvince(province);
            community.setIsDefaultCommunity(IsDefaultCommunityEnum.TRUE.getIsDefaultCommunity());
            community.setCommunityLevelId(CommunityLevelEnum.CITY.getLevelId());
            // 绑定上级社区
            cityCommunity.setParentCommunityId(provinceCommunity.getCommunityId());
            community.setCommunityName(province);
            community.setCommunityStatus(1);
            boolean save = this.save(community);
            ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR);
        }



        //3- 创建社区
        Community community = new Community();
        BeanUtils.copyProperties(communityRegisterRequest,community);
        community.setIsDefaultCommunity(IsDefaultCommunityEnum.FALSE.getIsDefaultCommunity());
        community.setCommunityLevelId(CommunityLevelEnum.STREET.getLevelId());
        community.setCommunityStatus(1);
        //绑定上级社区id
        community.setParentCommunityId(cityCommunity.getCommunityId());

        //- 检查是否存在对应的社区类别,将类别id存入
        String communityType = communityRegisterRequest.getCommunityType();
        CommunityTypeEnum communityTypeEnum = CommunityTypeEnum.getByName(communityType);
        ThrowUtils.throwIf(ObjUtil.isNull(communityTypeEnum),ErrorCode.PARAMS_ERROR,"社区类型输入错误");
        community.setCommunityTypeId(communityTypeEnum.getTypeId());

        //- 使用社区与创建人绑定管理表
        community.setRegisterVolunteerId(volunteer.getVolunteerId());



        this.save(community);
//        innerVolunteerService.updateByid();
//        communitymanagerMapper.insert();

        //4- 绑定职位表

        //5- 颁发token

        //6- 脱敏
        return null;
    }

    /**
     * 社区登录
     *
     * @param volunteerLARRequest 登录人手机号与密码
     * @return 脱敏后的登录数据
     */
    @Override
    public CommunityLoginSuccessVO communityLogin(VolunteerLARRequest volunteerLARRequest) {
        return null;
    }


    // region 工具方法


    /**
     * 通过社区名查询社区
     *
     * @param communityName 社区名
     * @return 社区
     */
    @Override
    public Community getByName(String communityName) {
        QueryWrapper<Community> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("community_name",communityName);
        Community community = this.getOne(queryWrapper);
        return community;
    }


    // endregion
}




