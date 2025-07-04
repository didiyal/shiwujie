package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.FamilyJoinReview;
import com.swj.shiwujie.service.FamilyJoinReviewService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 家庭接口
 * @author  ldl
 *
 */
@RestController
@CrossOrigin
@Slf4j
@RequestMapping("/familyJoinReview")
public class FamilyJoinReviewController {


    @Resource
    private FamilyJoinReviewService familyJoinReviewService;

    
    
//    /**
//     * 更新家庭信息
//     * @param familyJoinReviewUpdateRequest 家庭更新内容
//     * @param request 请求
//     * @return 更新后脱敏后的家庭信息
//     */
//    @PutMapping("/update")
//    public BaseResponse<FamilyJoinReview> updateFamilyJoinReview(FamilyJoinReviewUpdateRequest familyJoinReviewUpdateRequest, HttpServletRequest request) {
//        //1. 获取操作用户的id与手机号
//        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
//        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
//        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");
//
//
//        //2. 校验传入数据是否合法
//        Long familyJoinReviewId = familyJoinReviewUpdateRequest.getFamilyJoinReviewId();
//        ThrowUtils.throwIf(ObjUtil.isNull(familyJoinReviewUpdateRequest),ErrorCode.PARAMS_ERROR);
//        ThrowUtils.throwIf(ObjUtil.isNull(familyJoinReviewId),ErrorCode.PARAMS_ERROR);
//
//        //3. 检验
//        FamilyJoinReview result = familyJoinReviewService.updateFamilyJoinReview(familyJoinReviewUpdateRequest,loginVolunteerId,loginUserPhone);
//
//
//        return ResultUtils.success(result);
//    }

    


}
