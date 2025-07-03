package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.blind.BlindLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.blind.BlindUpdatePassword;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.domain.Blind;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.request.user.blind.BlindLARRequest;
import com.swj.shiwujie.model.request.user.blind.BlindUpdateRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.RedisUtils;
import com.swj.shiwujie.utils.ResultUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

import static com.swj.shiwujie.constants.UserConstants.PASSWORD_REGEX;
import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;

/**
 * 视障人士接口
 */
@RestController
@RequestMapping("/blind")
public class BlindController {

    @Resource
    private BlindService blindService;

    @Resource
    private RedisUtils redisUtils;


    // region 登录注册相关

    /**
     * 测试是否登录
     *
     * @return 登录用户id
     */
    @GetMapping("/login/check")
    public BaseResponse<BlindVO> checkLogin(@RequestHeader("Authorization") String token, HttpServletRequest request) {
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginBlindId), ErrorCode.NOT_LOGIN, "未登录");
        Blind blind = blindService.getById(loginBlindId);
        ThrowUtils.throwIf(ObjUtil.isNull(blind), ErrorCode.PARAMS_ERROR, "用户不存在");
        return ResultUtils.success(blindService.getBlindVO(blind));
    }


    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login/loginAndRegisterQuickly")
    public BaseResponse<BlindLoginSuccessVO> loginAndRegisterQuickly(String phone) {
        // 鉴空
        ThrowUtils.throwIf(StrUtil.isBlankIfStr(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        BlindLoginSuccessVO res = blindService.loginAndRegisterQuickly(phone);

        return ResultUtils.success(res);
    }


    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param blindLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login/loginAndRegister")
    public BaseResponse<BlindLoginSuccessVO> loginAndRegister(BlindLARRequest blindLARRequest) {
        ThrowUtils.throwIf(ObjUtil.hasEmpty(blindLARRequest), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        BlindLoginSuccessVO res = blindService.loginAndRegister(blindLARRequest);

        return ResultUtils.success(res);
    }


    // endregion

    // todo 实现volunteer的内容,修改volunteer的细节
    // todo 实现家庭与家庭申请的内容

    // region 增删改查

    /**
     * 删除用户
     *
     * @param blindId 用户id
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteBlind(Long blindId) {
        //校验id是否合法
        ThrowUtils.throwIf(ObjUtil.isNull(blindId) || blindId <= 0, ErrorCode.PARAMS_ERROR);

        boolean b = blindService.removeById(blindId);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);


        redisUtils.removeToRedis(REDIS_SECRETKEY + "-" + blindId);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     * 修改用户名,性别,身份证号,残疾人证
     * 后期可以修改经纬度与位置信息
     *
     * @param blindUpdateRequest 用户更新信息
     * @return 脱敏后的用户信息
     */
    @PostMapping("/update")
    public BaseResponse<BlindVO> updateBlind(@RequestBody BlindUpdateRequest blindUpdateRequest,HttpServletRequest request) {
        //校验参数是否为空
        ThrowUtils.throwIf(ObjUtil.isNull(blindUpdateRequest) || blindUpdateRequest.getBlindId() == null,
                ErrorCode.PARAMS_ERROR);
        // 只能自己修改自己的数据
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(!Objects.equals(loginBlindId, blindUpdateRequest.getBlindId()),ErrorCode.PARAMS_ERROR,"操作用户错误");

        Blind blind = new Blind();
        BeanUtils.copyProperties(blindUpdateRequest, blind);
        blind.setPhone(loginUserPhone);
        BlindVO result = blindService.updateBlind(blind);

        return ResultUtils.success(result);
    }

    /**
     * 修改手机号
     *
     * @param phone   要修改的手机号
     * @param request 请求
     * @return 脱敏后的用户信息
     */
    @PostMapping("/update/phone")
    public BaseResponse<BlindVO> updateBlindPhone(String phone,
                                                  HttpServletRequest request) {
        // 校验参数
        Long blindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "手机号格式错误");


        synchronized (loginUserPhone.intern()) {
            Blind blind = blindService.getById(blindId);

            // 更新
            blind.setPhone(phone);
            boolean b = blindService.updateById(blind);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

            BlindVO result = blindService.getBlindVO(blind);

            return ResultUtils.success(result);
        }

    }

    /**
     * 修改密码
     *
     * @param blindUpdatePassword 原密码与要修改的密码
     * @param request             请求
     * @return 是否成功
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updateBlindPassword(BlindUpdatePassword blindUpdatePassword,
                                                     HttpServletRequest request) {
        Long blindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        //鉴空
        ThrowUtils.throwIf(ObjUtil.hasEmpty(blindUpdatePassword), ErrorCode.PARAMS_ERROR);


        boolean result = blindService.updateBlindPassword(blindUpdatePassword,blindId,loginUserPhone);


        return ResultUtils.success(result);


    }


    /**
     * 根据ID查询用户
     *
     * @param blindId 用户id
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get/id")
    public BaseResponse<BlindVO> getBlindById(Long blindId,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(blindId == null || blindId <= 0, ErrorCode.PARAMS_ERROR);

        Blind blind = blindService.getById(blindId);
        // 脱敏
        BlindVO res = blindService.getBlindVO(blind);

        ThrowUtils.throwIf(ObjUtil.isNull(blind), ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(res);
    }


    // endregion

    /**
     * 加入社区
     * 根据id加入社区
     *
     * @param communityId 社区id
     * @return 是否成功
     */
    @GetMapping("/community/join")
    public BaseResponse<Boolean> joinCommunity(Long communityId,
                                               HttpServletRequest request) {

        Long blindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(communityId == null, ErrorCode.PARAMS_ERROR);

        synchronized (loginUserPhone.intern()) {
            Blind blind = blindService.getById(blindId);
            blind.setCommunityId(communityId);
            boolean result = blindService.updateById(blind);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            return ResultUtils.success(true);
        }
    }


}
