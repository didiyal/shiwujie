package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 社区管理接口
 */
@RestController
@RequestMapping("/community")
@Slf4j
public class CommunityController {


    @Resource
    private CommunityService communityService;


    /**
     * 测试是否登录
     *
     * @return 是否成功
     */
    @GetMapping("/login/check")
    public BaseResponse<Boolean> checkLogin(HttpServletRequest request) {
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");
        return ResultUtils.success(true);
    }


    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    @PostMapping("/Register")
    public BaseResponse<CommunityLoginSuccessVO> communityRegister(CommunityRegisterRequest communityRegisterRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(communityRegisterRequest), ErrorCode.PARAMS_ERROR,"信息填写不全");
        ThrowUtils.throwIf(ObjUtil.hasEmpty(communityRegisterRequest), ErrorCode.PARAMS_ERROR, "信息填写不全");

        CommunityLoginSuccessVO res = communityService.communityRegister(communityRegisterRequest);

        return ResultUtils.success(res);
    }




    /**
     * 社区管理人员登录
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @PostMapping("/Login")
    public BaseResponse<CommunityLoginSuccessVO> communityLogin(VolunteerLARRequest volunteerLARRequest) {
        ThrowUtils.throwIf(ObjUtil.hasEmpty(volunteerLARRequest), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        CommunityLoginSuccessVO res = communityService.communityLogin(volunteerLARRequest);

        return ResultUtils.success(res);
    }



}
