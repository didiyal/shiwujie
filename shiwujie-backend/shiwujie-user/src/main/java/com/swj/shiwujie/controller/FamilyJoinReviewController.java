package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO;
import com.swj.shiwujie.model.domain.user.Family;
import com.swj.shiwujie.model.domain.user.FamilyJoinReview;
import com.swj.shiwujie.model.request.user.familyJoinReview.FamilyJoinReviewUpdateRequest;
import com.swj.shiwujie.service.FamilyJoinReviewService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 审核表接口
 *
 * @author ldl
 */
@RestController
@Slf4j
@RequestMapping("/familyJoinReview")
@Api(tags = "家庭审核操作接口")
public class FamilyJoinReviewController {


    @Resource
    private FamilyJoinReviewService familyJoinReviewService;


    // region 用户操作

    /**
     * 更新审核信息
     *
     * @param familyJoinReviewUpdateRequest 审核更新内容
     * @param request   请求
     * @return 是否成功
     */
    @PutMapping("/update")
    @ApiOperation("更新家庭加入审核信息")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> updateFamilyJoinReview(FamilyJoinReviewUpdateRequest familyJoinReviewUpdateRequest, HttpServletRequest request) {
        //1. 获取操作用户的id
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);

        //2. 校验传入数据是否合法
        ThrowUtils.throwIf(ObjUtil.isNull(familyJoinReviewUpdateRequest), ErrorCode.PARAMS_ERROR);

        //3. 检验
        Boolean result = familyJoinReviewService.updateFamilyJoinReview(familyJoinReviewUpdateRequest, loginVolunteerId,loginUserPhone);


        return ResultUtils.success(result);
    }


    /**
     * 根据id获取审核封装信息
     *
     * @param reviewId 审核id
     * @return 脱敏后的审核信息
     */
    @GetMapping("/get/id/vo")
    @ApiOperation("根据ID获取家庭加入审核信息")
    public BaseResponse<FamilyJoinReviewVO> getFamilyJoinReviewVOById(Long reviewId) {

        FamilyJoinReview familyJoinReview = familyJoinReviewService.getById(reviewId);
        ThrowUtils.throwIf(ObjUtil.isNull(familyJoinReview), ErrorCode.PARAMS_ERROR, "审核信息不存在");
        FamilyJoinReviewVO result = familyJoinReviewService.getFamilyJoinReviewVO(familyJoinReview);

        return ResultUtils.success(result);


    }


    /**
     * 家主获取带审核封装信息列表
     *
     * @return 脱敏后的审核信息
     */
    @GetMapping("/get/list/vo")
    @ApiOperation("获取家庭加入审核信息列表")
    public BaseResponse<List<FamilyJoinReviewVO>> getFamilyJoinReviewVOList(HttpServletRequest request) {
        //1. 获取家主的id
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);


        List<FamilyJoinReviewVO> result = familyJoinReviewService.getFamilyJoinReviewVOList(loginVolunteerId);

        return ResultUtils.success(result);

    }


    // endregion


}
