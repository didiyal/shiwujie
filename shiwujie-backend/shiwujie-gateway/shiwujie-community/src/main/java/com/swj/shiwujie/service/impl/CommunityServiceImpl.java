package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.CommunityVolunteerRegisterRequest;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.InnerVolunteerService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

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


    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    @Override
    public CommunityLoginSuccessVO communityRegister(CommunityRegisterRequest communityRegisterRequest) {

        //- 检测社区名字是否存在
        String communityName = communityRegisterRequest.getCommunityName();
        QueryWrapper<Community> communityQueryWrapper = new QueryWrapper<>();
        communityQueryWrapper.eq("community_name",communityName);
        Community community = this.getOne(communityQueryWrapper);
        ThrowUtils.throwIf(ObjUtil.isNotNull(community),ErrorCode.PARAMS_ERROR,"该名字已存在");

        //- 判断社区注册人手机号格式是否正确,手机号是否存在
        CommunityVolunteerRegisterRequest requestVolunteer = communityRegisterRequest.getVolunteer();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(requestVolunteer),ErrorCode.PARAMS_ERROR,"信息填写不全");
        String phone = requestVolunteer.getPhone();
        Volunteer volunteer = innerVolunteerService.getByPhone(phone);
        //  - 存在
        if(ObjUtil.isNotNull(volunteer)){
            //    - 检查是否实名,没实名自动实名,身份证是否合法
        }
        //  - 不存在
        //    - 自动注册志愿者账号且实名,身份证是否合法
        //- 注册社区账号


        String province = communityRegisterRequest.getProvince();
        String city = communityRegisterRequest.getCity();
        String district = communityRegisterRequest.getDistrict();
        String address = communityRegisterRequest.getAddress();
        //- 检测是否有对应省份的上级账号
        //  - 存在
        //    - 将社区绑定上级社区
        //  - 不存在
        //    - 自动创建上级社区
        //      - 检查是否有这个省
        //      - 检查是否有这个市
        //    - 绑定新的上级社区
        //- 检查是否存在对应的社区类别
        //- 创建社区
        //- 使用社区与创建人绑定管理表
        return null;
    }


    // region 工具方法

    // endregion
}




