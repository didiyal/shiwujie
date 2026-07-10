package com.swj.shiwujie.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityEmployeeQueryRequest;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityManagerRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 社区管理人员接口
 */
@RestController
@RequestMapping("/communitymanager")
@Slf4j
@Tag(name = "社区管理人员操作接口")
public class CommunitymanagerController {

    @Resource
    private CommunitymanagerService communitymanagerService;

    @Resource
    private InnerVolunteerService innerVolunteerService;

    /**
     * 查询社区下的员工(志愿者)
     */
    @GetMapping("/employees")
    @Operation(summary = "查询社区下的员工(志愿者)")
    public BaseResponse<Page<VolunteerVO>> queryCommunityEmployees(CommunityEmployeeQueryRequest request) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull(request.getCommunityId()), ErrorCode.PARAMS_ERROR, "社区ID不能为空");
        Page<VolunteerVO> result = communitymanagerService.queryCommunityEmployees(request);
        return ResultUtils.success(result);
    }

    /**
     * 添加社区管理成员(志愿者)
     */
    @PostMapping("/manager/add")
    @Operation(summary = "添加社区管理成员(志愿者)")
    public BaseResponse<Boolean> addCommunityManager(@RequestBody CommunityManagerRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        boolean result = communitymanagerService.addCommunityManager(request, loginVolunteerId);
        return ResultUtils.success(result);
    }

    /**
     * 修改社区管理成员信息(志愿者)
     */
    @PutMapping("/manager/update")
    @Operation(summary = "修改社区管理成员信息(志愿者)")
    public BaseResponse<Boolean> updateCommunityManager(@RequestBody CommunityManagerRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        boolean result = communitymanagerService.updateCommunityManager(request, loginVolunteerId);
        return ResultUtils.success(result);
    }


    /**
     * 修改社区管理成员信息(志愿者)
     */
    @DeleteMapping("/manager/delete")
    @Operation(summary = "删除社区管理成员信息(志愿者)")
    public BaseResponse<Boolean> deleteCommunityManager(@RequestBody CommunityManagerRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        Volunteer volunteer = innerVolunteerService.getById(loginVolunteerId);
        int i = communitymanagerService.removeByVolunteerIdAndCommunityId(loginVolunteerId, volunteer.getCommunityId());
        ThrowUtils.throwIf(i<=0,ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }
}
