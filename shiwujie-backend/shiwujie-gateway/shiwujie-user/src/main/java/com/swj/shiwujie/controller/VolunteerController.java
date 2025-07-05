package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePasswordRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePhoneRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdateRequest;
import com.swj.shiwujie.service.VolunteerService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.RedisUtils;
import com.swj.shiwujie.utils.ResultUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;

/**
 * 视障人士接口
 */
@RestController
@RequestMapping("/volunteer")
public class VolunteerController {

    @Resource
    private VolunteerService volunteerService;

    @Resource
    private RedisUtils redisUtils;


    // region 登录注册相关

    /**
     * 测试是否登录
     *
     * @return 登录用户id
     */
    @GetMapping("/login/check")
    public BaseResponse<VolunteerVO> checkLogin(HttpServletRequest request) {
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");
        Volunteer volunteer = volunteerService.getById(loginVolunteerId);
        ThrowUtils.throwIf(ObjUtil.isNull(volunteer), ErrorCode.PARAMS_ERROR, "用户不存在");
        return ResultUtils.success(volunteerService.getVolunteerVO(volunteer));
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
    public BaseResponse<VolunteerLoginSuccessVO> loginAndRegisterQuickly(String phone) {
        // 鉴空
        ThrowUtils.throwIf(StrUtil.isBlankIfStr(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        VolunteerLoginSuccessVO res = volunteerService.loginAndRegisterQuickly(phone);

        return ResultUtils.success(res);
    }


    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login/loginAndRegister")
    public BaseResponse<VolunteerLoginSuccessVO> loginAndRegister(VolunteerLARRequest volunteerLARRequest) {
        ThrowUtils.throwIf(ObjUtil.hasEmpty(volunteerLARRequest), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        VolunteerLoginSuccessVO res = volunteerService.loginAndRegister(volunteerLARRequest);

        return ResultUtils.success(res);
    }



    /**
     * 注销登录
     *
     * @return 是否成功
     */
    @GetMapping("/login/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);

        redisUtils.removeToRedis(REDIS_SECRETKEY+"-volunteer-" + loginVolunteerId);

        return ResultUtils.success(true);
    }

    // endregion


    // region 增删改查

    /**
     * 删除用户(管理与用户皆可)
     *
     * @param volunteerId 用户id
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteVolunteer(Long volunteerId) {
        //校验id是否合法
        ThrowUtils.throwIf(ObjUtil.isNull(volunteerId) || volunteerId <= 0, ErrorCode.PARAMS_ERROR);

        boolean b = volunteerService.removeById(volunteerId);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);


        redisUtils.removeToRedis(REDIS_SECRETKEY + "-" + volunteerId);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     * 修改用户名,性别,身份证号,残疾人证
     * 后期可以修改经纬度与位置信息
     *
     * @param volunteerUpdateRequest 用户更新信息
     * @return 脱敏后的用户信息
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateVolunteer(@RequestBody VolunteerUpdateRequest volunteerUpdateRequest, HttpServletRequest request) {
        //校验参数是否为空
        ThrowUtils.throwIf(ObjUtil.isNull(volunteerUpdateRequest) || volunteerUpdateRequest.getVolunteerId() == null,
                ErrorCode.PARAMS_ERROR);
        // 只能自己修改自己的数据
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        ThrowUtils.throwIf(!Objects.equals(loginVolunteerId, volunteerUpdateRequest.getVolunteerId()), ErrorCode.PARAMS_ERROR, "操作用户错误");

        Volunteer volunteer = new Volunteer();
        BeanUtils.copyProperties(volunteerUpdateRequest, volunteer);
        Boolean result = volunteerService.updateVolunteer(volunteer);

        return ResultUtils.success(result);
    }

    /**
     * 修改手机号
     *
     * @param volunteerUpdatePhoneRequest 要修改的手机号
     * @param request                     请求
     * @return 脱敏后的用户信息
     */
    @PostMapping("/update/phone")
    public BaseResponse<Boolean> updateVolunteerPhone(VolunteerUpdatePhoneRequest volunteerUpdatePhoneRequest,
                                                      HttpServletRequest request) {
        // 校验参数
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long volunteerId = volunteerUpdatePhoneRequest.getVolunteerId();
        ThrowUtils.throwIf(!Objects.equals(loginVolunteerId, volunteerId), ErrorCode.PARAMS_ERROR, "只能修改自己的手机号");

        String phone = volunteerUpdatePhoneRequest.getPhone();
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "手机号格式错误");


        Volunteer volunteer = volunteerService.getById(volunteerId);

        // 更新
        volunteer.setPhone(phone);
        boolean b = volunteerService.updateById(volunteer);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);


    }

    /**
     * 修改密码
     *
     * @param volunteerUpdatePassword 原密码与要修改的密码
     * @return 是否成功
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updateVolunteerPassword(VolunteerUpdatePasswordRequest volunteerUpdatePassword) {
        //鉴空
        ThrowUtils.throwIf(ObjUtil.hasEmpty(volunteerUpdatePassword), ErrorCode.PARAMS_ERROR);


        boolean result = volunteerService.updateVolunteerPassword(volunteerUpdatePassword);


        return ResultUtils.success(result);


    }


    /**
     * 根据ID查询用户
     *
     * @param volunteerId 用户id
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get/id/vo")
    public BaseResponse<VolunteerVO> getVolunteerVOById(Long volunteerId,
                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(volunteerId == null || volunteerId <= 0, ErrorCode.PARAMS_ERROR);

        Volunteer volunteer = volunteerService.getById(volunteerId);
        // 脱敏
        VolunteerVO res = volunteerService.getVolunteerVO(volunteer);

        ThrowUtils.throwIf(ObjUtil.isNull(volunteer), ErrorCode.PARAMS_ERROR, "用户不存在");
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

        Long volunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(communityId == null, ErrorCode.PARAMS_ERROR);

        synchronized (loginUserPhone.intern()) {
            Volunteer volunteer = volunteerService.getById(volunteerId);
            volunteer.setCommunityId(communityId);
            boolean result = volunteerService.updateById(volunteer);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            return ResultUtils.success(true);
        }
    }


}
