package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.user.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.domain.Blind;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.domain.Volunteer;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.user.VolunteerLARRequest;

/**
* @author Administrator
* @description 针对表【Volunteer(志愿者信息表)】的数据库操作Service
* @createDate 2025-07-01 00:21:42
*/
public interface VolunteerService extends IService<Volunteer> {



    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    VolunteerLoginSuccessVO loginAndRegisterQuickly(String phone);



    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    VolunteerLoginSuccessVO loginAndRegister(VolunteerLARRequest volunteerLARRequest);


    // region 工具方法

    /**
     * 通过手机号查询盲人信息
     * @param phone 盲人手机号
     * @return 盲人信息
     */
    Blind getBlindByPhone(String phone);


    /**
     * 通过手机号查询用户(志愿者)信息
     * @param phone 志愿者手机号
     * @return 志愿者信息
     */
    Volunteer getByPhone(String phone);


    /**
     * 用户注册登录返回信息脱敏
     * @param newVolunteer 返回的志愿者信息
     * @param token 登录token
     * @return 脱敏后的对象
     */
    VolunteerLoginSuccessVO getLoginSuccessVO(Volunteer newVolunteer, String token);

    /**
     * 登录成功实现令牌生成与redis储存
     * @param volunteer 志愿者信息
     * @return token
     */
    String loginSuccess(Volunteer volunteer);


    // endregion
}
