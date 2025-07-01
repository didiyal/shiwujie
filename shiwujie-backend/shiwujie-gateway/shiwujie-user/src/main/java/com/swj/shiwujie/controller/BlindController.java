package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.user.BlindLoginSuccessVO;
import com.swj.shiwujie.model.request.user.BlindLARRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.utils.ResultUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user/blind")
public class BlindController {

    @Resource
    private BlindService blindService;


    /**
     * 测试是否登录
     * @return
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
     * @param phone
     * @return
     */
    @PostMapping("/loginAndRegisterQuickly")
    public BaseResponse<BlindLoginSuccessVO> loginAndRegisterQuickly(String phone){
        if(StrUtil.isBlankIfStr(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }

        BlindLoginSuccessVO res = blindService.loginAndRegisterQuickly(phone);

        return ResultUtils.success(res);
    }



    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param blindLARRequest
     * @return
     */
    @PostMapping("/loginAndRegister")
    public BaseResponse<BlindLoginSuccessVO> loginAndRegister(BlindLARRequest blindLARRequest){
        if(ObjUtil.hasEmpty(blindLARRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }

        BlindLoginSuccessVO res = blindService.loginAndRegister(blindLARRequest);

        return ResultUtils.success(res);
    }

}
