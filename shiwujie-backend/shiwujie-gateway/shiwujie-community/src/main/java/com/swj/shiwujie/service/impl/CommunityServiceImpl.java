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
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityLevelEnum;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.enums.community.CommunityTypeEnum;
import com.swj.shiwujie.model.enums.community.IsDefaultCommunityEnum;
import com.swj.shiwujie.model.request.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.CommunityVolunteerRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;
import static com.swj.shiwujie.constants.UserConstants.TOKEN_SECRETKEY;

/**
 * @author Administrator
 * @description 针对表【Community(社区信息表)】的数据库操作Service实现
 * @createDate 2025-07-18 14:44:37
 */
@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper, Community>
        implements CommunityService {


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

        //todo 处理分布式事务
        //1- 判断社区注册人手机号格式是否正确,手机号是否存在
        CommunityVolunteerRegisterRequest requestVolunteer = communityRegisterRequest.getVolunteer();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(requestVolunteer), ErrorCode.PARAMS_ERROR, "信息填写不全");
        String phone = requestVolunteer.getPhone();
        Volunteer volunteer = innerVolunteerService.getByPhone(phone);
        if (ObjUtil.isNull(volunteer)) {
            //  - 不存在
            //    - 自动注册志愿者账号且实名,身份证是否合法
            volunteer = new Volunteer();
            BeanUtils.copyProperties(requestVolunteer, volunteer);
            boolean save = innerVolunteerService.save(volunteer);
        }
        //  - 存在
        //    - 检查是否实名,没实名自动实名,身份证是否合法
        String idCard = requestVolunteer.getIdCard();
        if (IdcardUtil.isValidCard(idCard)) {
            volunteer.setIdCard(SecureUtil.md5(idCard));
        }


        //2- 检测是否有对应省份的上级账号
        // 检查省
        String province = communityRegisterRequest.getProvince();
        Community provinceCommunity = this.getByName(province);
        if (ObjUtil.isNull(provinceCommunity)) {
            //  - 不存在
            //    - 自动创建上级社区
            provinceCommunity = new Community();
            provinceCommunity.setProvince(province);
            provinceCommunity.setCommunityLevelId(CommunityLevelEnum.PROVINCE.getLevelId());
            provinceCommunity.setIsDefaultCommunity(IsDefaultCommunityEnum.TRUE.getIsDefaultCommunity());
            provinceCommunity.setCommunityName(province);
            provinceCommunity.setCommunityStatus(1);
            boolean save = this.save(provinceCommunity);
            ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
        }
        // 检查市
        String city = communityRegisterRequest.getCity();
        Community cityCommunity = this.getByName(city);
        if (ObjUtil.isNull(cityCommunity)) {
            cityCommunity = new Community();
            cityCommunity.setProvince(province);
            cityCommunity.setCity(city);
            cityCommunity.setIsDefaultCommunity(IsDefaultCommunityEnum.TRUE.getIsDefaultCommunity());
            cityCommunity.setCommunityLevelId(CommunityLevelEnum.CITY.getLevelId());
            // 绑定上级社区
            cityCommunity.setParentCommunityId(provinceCommunity.getCommunityId());
            cityCommunity.setCommunityName(city);
            cityCommunity.setCommunityStatus(1);
            boolean save = this.save(cityCommunity);
            ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
        }


        //3- 创建社区
        Community community = new Community();
        BeanUtils.copyProperties(communityRegisterRequest, community);
        community.setIsDefaultCommunity(IsDefaultCommunityEnum.FALSE.getIsDefaultCommunity());
        community.setCommunityLevelId(CommunityLevelEnum.STREET.getLevelId());
        community.setCommunityStatus(1);
        //绑定上级社区id
        community.setParentCommunityId(cityCommunity.getCommunityId());

        //- 检查是否存在对应的社区类别,将类别id存入
        String communityType = communityRegisterRequest.getCommunityType();
        CommunityTypeEnum communityTypeEnum = CommunityTypeEnum.getByName(communityType);
        ThrowUtils.throwIf(ObjUtil.isNull(communityTypeEnum), ErrorCode.PARAMS_ERROR, "社区类型输入错误");
        community.setCommunityTypeId(communityTypeEnum.getTypeId());

        //- 使用社区与创建人绑定管理表
        community.setRegisterVolunteerId(volunteer.getVolunteerId());

        boolean save = this.save(community);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "社区创建失败");
        volunteer.setCommunityId(community.getCommunityId());
        innerVolunteerService.updateById(volunteer);

        //4- 绑定职位表
        Communitymanager communitymanager = new Communitymanager();
        communitymanager.setCommunityId(community.getCommunityId());
        communitymanager.setVolunteerId(volunteer.getVolunteerId());
        communitymanager.setRolePermissionId(CommunityRolePermissionEnum.REGISTRANT.getRoleId());

        int insert = communitymanagerMapper.insert(communitymanager);
        ThrowUtils.throwIf(insert <= 0, ErrorCode.SYSTEM_ERROR, "管理员绑定失败");


        //5- 构建并返回结果
        return buildCommunityLoginResult(community, volunteer);


    }

    /**
     * 社区登录
     *
     * @param volunteerLARRequest 登录人手机号与密码
     * @return 脱敏后的登录数据
     */
    @Override
    public CommunityLoginSuccessVO communityLogin(VolunteerLARRequest volunteerLARRequest) {
        ThrowUtils.throwIf(ObjUtil.hasEmpty(volunteerLARRequest.getPhone(), volunteerLARRequest.getPassword()),
                ErrorCode.PARAMS_ERROR, "手机号或密码不能为空");

        // 1. 根据手机号查询志愿者
        Volunteer volunteer = innerVolunteerService.getByPhone(volunteerLARRequest.getPhone());
        ThrowUtils.throwIf(ObjUtil.isNull(volunteer), ErrorCode.OPERATION_ERROR, "用户不存在");

        // 2. 验证密码
        String encryptPassword = SecureUtil.md5(volunteerLARRequest.getPassword());
        ThrowUtils.throwIf(!encryptPassword.equals(volunteer.getPassword()),
                ErrorCode.PARAMS_ERROR, "密码错误");

        // 3. 检查该志愿者是否为社区管理员
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteer.getVolunteerId()).eq("community_id",volunteer.getCommunityId());
        Communitymanager communitymanager = communitymanagerMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.NO_AUTH, "无社区管理权限");

        // 4. 获取社区信息
        Community community = this.getById(communitymanager.getCommunityId());
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.OPERATION_ERROR, "社区不存在");

        //5. 构建并返回结果
        return buildCommunityLoginResult(community, volunteer);
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
        queryWrapper.eq("community_name", communityName);
        Community community = this.getOne(queryWrapper);
        return community;
    }


    /**
     * 构建社区登录注册返回结果
     */
    private CommunityLoginSuccessVO buildCommunityLoginResult(Community community, Volunteer volunteer) {
        CommunityLoginSuccessVO result = new CommunityLoginSuccessVO();
        // 从 community 对象中获取对应属性值并设置到 result 中
        result.setCommunityTypeName(community.getCommunityTypeId() != null ? CommunityTypeEnum.getById(community.getCommunityTypeId()).getName() : null);
        result.setCommunityLevelName(community.getCommunityLevelId() != null ? CommunityLevelEnum.getById(community.getCommunityLevelId()).getName() : null);
        result.setParentCommunityId(community.getParentCommunityId());
        result.setCommunityDescription(community.getCommunityDescription());
        result.setProvince(community.getProvince());
        result.setCity(community.getCity());
        result.setDistrict(community.getDistrict());
        result.setAddress(community.getAddress());
        result.setRegistrationInfo(community.getRegistrationInfo());
        result.setRegisterVolunteerId(community.getRegisterVolunteerId());
        result.setCommunityStatus(community.getCommunityStatus());

        result.setCommunityId(community.getCommunityId());
        result.setCommunityName(community.getCommunityName());
        result.setToken(innerVolunteerService.generateLoginToken(volunteer));
        return result;
    }

    // endregion
}




