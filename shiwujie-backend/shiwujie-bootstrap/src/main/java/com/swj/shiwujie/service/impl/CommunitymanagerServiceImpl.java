package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.mapper.CommunitymanagerMapper;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityEmployeeQueryRequest;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityManagerRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service实现
* @createDate 2025-07-19 01:31:15
*/
@Service
public class CommunitymanagerServiceImpl extends ServiceImpl<CommunitymanagerMapper, Communitymanager> implements CommunitymanagerService {

    @Resource
    private CommunitymanagerMapper communitymanagerMapper;

    @Resource
    private CommunityMapper communityMapper;

    @Resource
    private InnerVolunteerService innerVolunteerService;





    /**
     * 添加社区管理成员(志愿者)
     */
    @Override
    public Page<VolunteerVO> queryCommunityEmployees(CommunityEmployeeQueryRequest request) {
        Long communityId = request.getCommunityId();
        long current = request.getCurrent();
        long size = request.getPageSize();
        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(current <= 0 || size <= 0 || size > 100, ErrorCode.PARAMS_ERROR, "分页参数不合法");

        Community community = communityMapper.selectById(communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.PARAMS_ERROR, "社区不存在");

        // 分页查询社区管理员记录
        Page<Communitymanager> page = new Page<>(current, size);
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("community_id", communityId);
        Page<Communitymanager> communitymanagerPage = communitymanagerMapper.selectPage(page, queryWrapper);

        List<Communitymanager> communitymanagerList = communitymanagerPage.getRecords();
        List<VolunteerVO> volunteerVOList = new ArrayList<>();
        if (!communitymanagerList.isEmpty()) {
            List<Long> volunteerIdList = communitymanagerList.stream()
                    .map(Communitymanager::getVolunteerId)
                    .collect(Collectors.toList());

            for (Long volunteerId : volunteerIdList) {
                Volunteer volunteer = innerVolunteerService.getById(volunteerId);
                if (ObjUtil.isNotNull(volunteer)) {
                    volunteerVOList.add(innerVolunteerService.getVolunteerVO(volunteer));
                }
            }
        }

        Page<VolunteerVO> resultPage = new Page<>(current, size, communitymanagerPage.getTotal());
        resultPage.setRecords(volunteerVOList);
        return resultPage;
    }


    /**
     * 添加社区管理成员(志愿者)
     */
    @Override
    public boolean addCommunityManager(CommunityManagerRequest request, Long loginVolunteerId) {
        Long communityId = request.getCommunityId();
        Long volunteerId = request.getVolunteerId();
        String roleName = request.getRoleName();

        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(volunteerId == null || volunteerId <= 0, ErrorCode.PARAMS_ERROR, "志愿者ID不合法");
        ThrowUtils.throwIf(ObjUtil.isNull(roleName), ErrorCode.PARAMS_ERROR, "社区管理身份不能为空");

        Community community = communityMapper.selectById(communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.PARAMS_ERROR, "社区不存在");

        // 权限校验：仅注册人/管理员可增删改成员（员工无权）
        assertCommunityAdmin(loginVolunteerId, communityId);

        Volunteer volunteer = innerVolunteerService.getById(volunteerId);
        ThrowUtils.throwIf(ObjUtil.isNull(volunteer), ErrorCode.PARAMS_ERROR, "志愿者不存在");

        CommunityRolePermissionEnum roleEnum = CommunityRolePermissionEnum.getByName(roleName);
        ThrowUtils.throwIf(ObjUtil.isNull(roleEnum), ErrorCode.PARAMS_ERROR, "社区管理身份不合法");
        // 注册人仅社区创建时产生一个，不可后增
        ThrowUtils.throwIf(roleEnum == CommunityRolePermissionEnum.REGISTRANT, ErrorCode.NO_AUTH, "注册人不可新增");

        Communitymanager existingManager = this.getByVolunteerIdAndCommunityId(volunteerId, communityId);
        ThrowUtils.throwIf(ObjUtil.isNotNull(existingManager), ErrorCode.OPERATION_ERROR, "该志愿者已为社区管理人员");

        Communitymanager communitymanager = new Communitymanager();
        communitymanager.setCommunityId(communityId);
        communitymanager.setVolunteerId(volunteerId);
        communitymanager.setRolePermissionId(roleEnum.getRoleId());

        return communitymanagerMapper.insert(communitymanager) > 0;
    }


    /**
     * 修改社区管理成员信息(志愿者)
     */
    @Override
    public boolean updateCommunityManager(CommunityManagerRequest request, Long loginVolunteerId) {
        Long communityId = request.getCommunityId();
        Long volunteerId = request.getVolunteerId();
        String roleName = request.getRoleName();

        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(volunteerId == null || volunteerId <= 0, ErrorCode.PARAMS_ERROR, "志愿者ID不合法");
        ThrowUtils.throwIf(ObjUtil.isNull(roleName), ErrorCode.PARAMS_ERROR, "社区管理身份不能为空");

        Community community = communityMapper.selectById(communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.PARAMS_ERROR, "社区不存在");

        // 权限校验：仅注册人/管理员可增删改成员（员工无权）
        assertCommunityAdmin(loginVolunteerId, communityId);

        CommunityRolePermissionEnum roleEnum = CommunityRolePermissionEnum.getByName(roleName);
        ThrowUtils.throwIf(ObjUtil.isNull(roleEnum), ErrorCode.PARAMS_ERROR, "社区管理身份不合法");

        Communitymanager communitymanager = getByVolunteerIdAndCommunityId(volunteerId, communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.PARAMS_ERROR, "管理记录不存在");

        // 末位注册人护栏：不允许把唯一的注册人降级/改走
        if (communitymanager.getRolePermissionId() == CommunityRolePermissionEnum.REGISTRANT.getRoleId()
                && roleEnum != CommunityRolePermissionEnum.REGISTRANT) {
            long registrantCount = countByCommunityIdAndRole(communityId, CommunityRolePermissionEnum.REGISTRANT.getRoleId());
            ThrowUtils.throwIf(registrantCount <= 1, ErrorCode.NO_AUTH, "不可降级末位注册人");
        }

        communitymanager.setRolePermissionId(roleEnum.getRoleId());

        return communitymanagerMapper.updateById(communitymanager) > 0;
    }

    /**
     * 删除社区管理成员(志愿者)
     */
    @Override
    public boolean deleteCommunityManager(Long targetVolunteerId, Long communityId, Long loginVolunteerId) {
        ThrowUtils.throwIf(targetVolunteerId == null || targetVolunteerId <= 0, ErrorCode.PARAMS_ERROR, "志愿者ID不合法");
        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");

        // 权限校验：仅注册人/管理员可删除成员（员工无权）
        assertCommunityAdmin(loginVolunteerId, communityId);

        // 末位注册人护栏：不允许删除唯一的注册人
        Communitymanager target = this.getByVolunteerIdAndCommunityId(targetVolunteerId, communityId);
        ThrowUtils.throwIf(ObjUtil.isNull(target), ErrorCode.PARAMS_ERROR, "管理记录不存在");
        if (target.getRolePermissionId() == CommunityRolePermissionEnum.REGISTRANT.getRoleId()) {
            long registrantCount = countByCommunityIdAndRole(communityId, CommunityRolePermissionEnum.REGISTRANT.getRoleId());
            ThrowUtils.throwIf(registrantCount <= 1, ErrorCode.NO_AUTH, "不可删除末位注册人");
        }

        int deleted = this.removeByVolunteerIdAndCommunityId(targetVolunteerId, communityId);
        return deleted > 0;
    }

    /**
     * 通过志愿者id,社区id查询信息数量
     *
     * @param volunteerId
     * @param communityId
     */
    @Override
    public long getCountByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId)
                .eq("community_id", communityId);
        return communitymanagerMapper.selectCount(queryWrapper);
    }


    //region 工具方法

    /**
     * 通过志愿者id,社区id查询信息
     */
    @Override
    public Communitymanager getByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId)
                .eq("community_id", communityId);
        return communitymanagerMapper.selectOne(queryWrapper);
    }

    /**
     * 删除信息
     *
     * @param volunteerId
     * @param communityId
     * @return
     */
    @Override
    public int removeByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId)
                .eq("community_id", communityId);
        return communitymanagerMapper.delete(queryWrapper);
    }

    /**
     * 根据社区ID删除管理记录
     * @param communityId 社区ID
     * @return 删除数量
     */
    @Override
    public int removeByCommunityId(Long communityId) {
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("community_id", communityId);
        return communitymanagerMapper.delete(queryWrapper);
    }

    /**
     * 权限校验：操作人须为该社区的注册人或管理员（员工无权）
     */
    private void assertCommunityAdmin(Long volunteerId, Long communityId) {
        ThrowUtils.throwIf(volunteerId == null || communityId == null, ErrorCode.NOT_LOGIN, "未登录");
        Communitymanager communitymanager = this.getByVolunteerIdAndCommunityId(volunteerId, communityId);
        ThrowUtils.throwIf(communitymanager == null
                        || communitymanager.getRolePermissionId() == CommunityRolePermissionEnum.EMPLOYEE.getRoleId(),
                ErrorCode.NO_AUTH, "无权限管理社区成员");
    }

    /**
     * 统计某社区指定角色的管理成员数
     */
    private long countByCommunityIdAndRole(Long communityId, long roleId) {
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("community_id", communityId).eq("role_permission_id", roleId);
        return communitymanagerMapper.selectCount(queryWrapper);
    }

    //endregion


}




