package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.UserConstants;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.mapper.CommunitymanagerMapper;
import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.VO.community.CommunityVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityLevelEnum;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.enums.community.CommunityTypeEnum;
import com.swj.shiwujie.model.enums.community.IsDefaultCommunityEnum;
import com.swj.shiwujie.model.request.community.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.community.community.CommunityUpdateRequest;
import com.swj.shiwujie.model.request.user.volunteer.CommunityVolunteerRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.utils.RedisUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;

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

    @DubboReference
    private InnerBlindService innerBlindService;


    @Resource
    private RedisUtils redisUtils;

    @Resource
    private CommunitymanagerService communitymanagerService;


    //region 注册登录

    /**
     * 测试登录
     *
     * @param loginVolunteerId
     */
    @Override
    public void checkLogin(Long loginVolunteerId) {
        Volunteer volunteer = innerVolunteerService.getById(loginVolunteerId);
        ThrowUtils.throwIf(ObjUtil.isNull(volunteer.getCommunityId()),ErrorCode.NO_AUTH);
        Communitymanager communitymanager = communitymanagerService.getByVolunteerIdAndCommunityId(
                volunteer.getVolunteerId(), volunteer.getCommunityId());
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager),ErrorCode.NO_AUTH);
    }

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

        boolean insert = communitymanagerService.save(communitymanager);
        ThrowUtils.throwIf(!insert, ErrorCode.SYSTEM_ERROR, "管理员绑定失败");


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
        Communitymanager communitymanager = communitymanagerService.getOne(queryWrapper);
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.NO_AUTH, "无社区管理权限");

        // 4. 获取社区信息
        Community community = this.getById(communitymanager.getCommunityId());
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.OPERATION_ERROR, "社区不存在");

        //5. 构建并返回结果
        return buildCommunityLoginResult(community, volunteer);
    }

    //endregion

    /**
     * 修改社区信息
     * @param request 修改请求
     * @param volunteerId 操作人ID
     * @return 更新后的社区信息
     */
    @Override
    public CommunityVO updateCommunity(CommunityUpdateRequest request, Long volunteerId) {
        Long communityId = request.getCommunityId();
        String communityName = request.getCommunityName();
        String communityDescription = request.getCommunityDescription();

        // 检查社区是否存在
        Community community = this.getById(communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.PARAMS_ERROR, "社区不存在");

        // 检查权限(只有注册人)
        Communitymanager communitymanager = communitymanagerService.getByVolunteerIdAndCommunityId(volunteerId, communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.NO_AUTH, "无权限操作该社区");
        Long roleId = communitymanager.getRolePermissionId();
        ThrowUtils.throwIf(!roleId.equals(CommunityRolePermissionEnum.REGISTRANT.getRoleId()), ErrorCode.NO_AUTH, "无权限修改社区信息");

        // 更新字段（仅非空值）
        if (ObjUtil.isNotNull(communityName) && !communityName.trim().isEmpty()) {
            community.setCommunityName(communityName);
        }
        if (ObjUtil.isNotNull(communityDescription) && !communityDescription.trim().isEmpty()) {
            community.setCommunityDescription(communityDescription);
        }

        boolean updateResult = this.updateById(community);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "社区信息更新失败");

        return this.getCommunityVO(community);
    }

    /**
     * 删除社区
     * @param communityId 社区ID
     * @param volunteerId 操作人ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteCommunity(Long communityId, Long volunteerId) {
        // 参数校验
        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(volunteerId == null || volunteerId <= 0, ErrorCode.PARAMS_ERROR, "操作人ID不合法");

        // 检查社区是否存在
        Community community = this.getById(communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.PARAMS_ERROR, "社区不存在");

        // 检查权限（必须是注册人）
        Communitymanager communitymanager = communitymanagerService.getByVolunteerIdAndCommunityId(volunteerId, communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.NO_AUTH, "无权限操作该社区");
        Long roleId = communitymanager.getRolePermissionId();
        ThrowUtils.throwIf(!roleId.equals(CommunityRolePermissionEnum.REGISTRANT.getRoleId()), ErrorCode.NO_AUTH, "只有社区注册人可删除社区");

        // 将社区内所有视障人士移出社区
        boolean blindResult = innerBlindService.removeCommunityId(communityId);
        ThrowUtils.throwIf(!blindResult, ErrorCode.SYSTEM_ERROR, "视障人士移出社区失败");

        // 将社区内所有志愿者移出社区
        boolean volunteerResult = innerVolunteerService.removeCommunityId(communityId);
        ThrowUtils.throwIf(!volunteerResult, ErrorCode.SYSTEM_ERROR, "志愿者移出社区失败");

        // 删除社区管理记录
        int managerDeleteCount = communitymanagerService.removeByCommunityId(communityId);
        ThrowUtils.throwIf(managerDeleteCount < 0, ErrorCode.SYSTEM_ERROR, "删除社区管理记录失败");

        // 删除社区
        boolean deleteResult = this.removeById(communityId);
        ThrowUtils.throwIf(!deleteResult, ErrorCode.SYSTEM_ERROR, "删除社区失败");

        return true;
    }

    /**
     * 分页查询社区下的子社区
     * @param communityId 父社区ID
     * @param current 页码
     * @param size 每页条数
     * @param volunteerId 操作人ID
     * @return 子社区列表
     */
    @Override
    public List<CommunityVO> getSubCommunities(Long communityId, long current, long size, Long volunteerId) {
        // 参数校验
        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(current <= 0 || size <= 0 || size > 100, ErrorCode.PARAMS_ERROR, "分页参数不合法");
        ThrowUtils.throwIf(volunteerId == null || volunteerId <= 0, ErrorCode.PARAMS_ERROR, "操作人ID不合法");

        // 检查父社区是否存在
        Community parentCommunity = this.getById(communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(parentCommunity), ErrorCode.PARAMS_ERROR, "父社区不存在");

        // 检查权限（注册人或管理员）
        Communitymanager communitymanager = communitymanagerService.getByVolunteerIdAndCommunityId(volunteerId, communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.NO_AUTH, "无权限操作该社区");
        Long roleId = communitymanager.getRolePermissionId();
        ThrowUtils.throwIf(!(roleId.equals(CommunityRolePermissionEnum.REGISTRANT.getRoleId()) ||
                roleId.equals(CommunityRolePermissionEnum.ADMIN.getRoleId())), ErrorCode.NO_AUTH, "无权限查看子社区");

        // 分页查询子社区
        Page<Community> page = new Page<>(current, size);
        QueryWrapper<Community> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_community_id", communityId)
                .orderByDesc("create_time");
        Page<Community> communityPage = this.page(page, queryWrapper);

        // 转换为VO并返回
        return communityPage.getRecords().stream()
                .map(this::getCommunityVO)
                .collect(Collectors.toList());
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
        return this.getOne(queryWrapper);
    }


    /**
     * 构建社区登录注册返回结果
     */
    @Override
    public CommunityLoginSuccessVO buildCommunityLoginResult(Community community, Volunteer volunteer) {
        CommunityLoginSuccessVO result = new CommunityLoginSuccessVO();
        result.setVolunteer(innerVolunteerService.getVolunteerVO(volunteer));
        //检查是否有token
        String token = (String)redisUtils.getFromRedis(REDIS_SECRETKEY + "-volunteer-" + volunteer.getVolunteerId());
        if(ObjUtil.isNotNull(token)){
            result.setToken(token);
        }else{
            //不存在
            result.setToken(innerVolunteerService.generateLoginToken(volunteer));
        }

        return result;
    }





    /**
     * 封装脱敏
     *
     * @param community
     * @return
     */
    @Override
    public CommunityVO getCommunityVO(Community community) {
        CommunityVO communityVO = new CommunityVO();
        communityVO.setCommunityId(community.getCommunityId());
        communityVO.setCommunityTypeName(community.getCommunityTypeId() == null
                ? null : CommunityTypeEnum.getById(community.getCommunityTypeId()).getName());
        communityVO.setCommunityLevelName(community.getCommunityLevelId() == null
                ? null : CommunityLevelEnum.getById(community.getCommunityLevelId()).getName());
        communityVO.setParentCommunityId(community.getParentCommunityId());
        communityVO.setCommunityName(community.getCommunityName());
        communityVO.setCommunityDescription(community.getCommunityDescription());
        communityVO.setProvince(community.getProvince());
        communityVO.setCity(community.getCity());
        communityVO.setDistrict(community.getDistrict());
        communityVO.setAddress(community.getAddress());
        communityVO.setRegistrationInfo(community.getRegistrationInfo());
        communityVO.setRegisterVolunteerId(community.getRegisterVolunteerId());
        communityVO.setCommunityStatus(community.getCommunityStatus());
        return communityVO;
    }

    // endregion
}




