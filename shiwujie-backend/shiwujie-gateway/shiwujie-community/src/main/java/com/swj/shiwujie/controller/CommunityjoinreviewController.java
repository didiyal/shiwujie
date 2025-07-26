package com.swj.shiwujie.controller;

import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.communityJoinReview.CommunityJoinReviewVO;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.request.community.communityJoinReview.CommunityJoinReviewUpdateRequest;
import com.swj.shiwujie.service.CommunityjoinreviewService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 社区加入审核接口
 */
@RestController
@RequestMapping("/communityjoinreview")
@Slf4j
public class CommunityjoinreviewController {

    @Resource
    private CommunityjoinreviewService communityjoinreviewService;

    // region 管理员操作

    /**
     * 更新社区加入审核状态
     *
     * @param updateRequest 审核更新请求
     * @param request       请求
     * @return 是否成功
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateCommunityJoinReview(@RequestBody CommunityJoinReviewUpdateRequest updateRequest, HttpServletRequest request) {
        // 获取登录管理员ID
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long volunteerRole = LoginUtils.getVolunteerRole(request);
        CommunityRolePermissionEnum rolePermissionEnum = CommunityRolePermissionEnum.getById(volunteerRole);
        ThrowUtils.throwIf(rolePermissionEnum.equals(CommunityRolePermissionEnum.EMPLOYEE),ErrorCode.NO_AUTH,"社区权限不足");
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);

        ThrowUtils.throwIf(ObjUtil.isNull(updateRequest), ErrorCode.PARAMS_ERROR);
        boolean result = communityjoinreviewService.updateCommunityJoinReview(updateRequest, loginVolunteerId, loginUserPhone);
        return ResultUtils.success(result);
    }

    /**
     * 获取社区待审核列表
     *
     * @param request 请求
     * @return 审核列表
     */
    @GetMapping("/get/list/vo")
    public BaseResponse<List<CommunityJoinReviewVO>> getCommunityJoinReviewVOList(HttpServletRequest request) {
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long volunteerRole = LoginUtils.getVolunteerRole(request);
        CommunityRolePermissionEnum rolePermissionEnum = CommunityRolePermissionEnum.getById(volunteerRole);
        ThrowUtils.throwIf(rolePermissionEnum.equals(CommunityRolePermissionEnum.EMPLOYEE),ErrorCode.NO_AUTH,"社区权限不足");

        List<CommunityJoinReviewVO> result = communityjoinreviewService.getCommunityJoinReviewVOList(loginVolunteerId);
        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取社区审核详情
     *
     * @param reviewId 审核ID
     * @return 脱敏后的审核信息
     */
    @GetMapping("/get/id/vo")
    public BaseResponse<CommunityJoinReviewVO> getCommunityJoinReviewVOById(Long reviewId,HttpServletRequest request) {
        Long volunteerRole = LoginUtils.getVolunteerRole(request);
        CommunityRolePermissionEnum rolePermissionEnum = CommunityRolePermissionEnum.getById(volunteerRole);
        ThrowUtils.throwIf(rolePermissionEnum.equals(CommunityRolePermissionEnum.EMPLOYEE),ErrorCode.NO_AUTH,"社区权限不足");
        ThrowUtils.throwIf(reviewId == null || reviewId <= 0, ErrorCode.PARAMS_ERROR, "审核ID不合法");

        CommunityJoinReviewVO result = communityjoinreviewService.getCommunityJoinReviewVOById(reviewId);
        return ResultUtils.success(result);
    }

    // endregion

}
