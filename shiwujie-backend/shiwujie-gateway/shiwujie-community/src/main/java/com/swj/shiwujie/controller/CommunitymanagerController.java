package com.swj.shiwujie.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityEmployeeQueryRequest;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityManagerRequest;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 社区管理人员接口
 */
@RestController
@RequestMapping("/communitymanager")
@Slf4j
public class CommunitymanagerController {

    @Resource
    private CommunitymanagerService communitymanagerService;

    /**
     * 查询社区下的员工(志愿者)
     */
    @GetMapping("/employees")
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
    public BaseResponse<Boolean> updateCommunityManager(@RequestBody CommunityManagerRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR);
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        boolean result = communitymanagerService.updateCommunityManager(request, loginVolunteerId);
        return ResultUtils.success(result);
    }
}
