package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.user.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.request.user.VolunteerLARRequest;
import com.swj.shiwujie.service.VolunteerService;
import com.swj.shiwujie.utils.ResultUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 志愿者接口
 */
@RestController
@RequestMapping("/user/volunteer")
public class VolunteerController {

    @Resource
    private VolunteerService volunteerService;

    // region 登录注册相关

    /**
     * 测试是否登录
     * @return 登录用户id
     */
    @GetMapping("/check")
    public BaseResponse<Long> checkLogin(@RequestHeader("Authorization") String token,HttpServletRequest request){
        Long loginUserId = (Long) request.getAttribute("loginUserId");
        if(loginUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        return ResultUtils.success(loginUserId);
    }



    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    @PostMapping("/loginAndRegisterQuickly")
    public BaseResponse<VolunteerLoginSuccessVO> loginAndRegisterQuickly(String phone){
        if(StrUtil.isBlankIfStr(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }

        VolunteerLoginSuccessVO res = volunteerService.loginAndRegisterQuickly(phone);

        return ResultUtils.success(res);
    }



    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @PostMapping("/loginAndRegister")
    public BaseResponse<VolunteerLoginSuccessVO> loginAndRegister(VolunteerLARRequest volunteerLARRequest){
        if(ObjUtil.hasEmpty(volunteerLARRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }

        VolunteerLoginSuccessVO res = volunteerService.loginAndRegister(volunteerLARRequest);

        return ResultUtils.success(res);
    }

    // endregion
}
