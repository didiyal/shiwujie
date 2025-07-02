package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.user.BlindLoginSuccessVO;
import com.swj.shiwujie.model.domain.Blind;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.request.user.BlindLARRequest;

/**
* @author Administrator
* @description 针对表【Blind(视障人士信息表)】的数据库操作Service
* @createDate 2025-07-01 00:21:42
*/
public interface BlindService extends IService<Blind> {


    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    BlindLoginSuccessVO loginAndRegisterQuickly(String phone);



    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param blindLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    BlindLoginSuccessVO loginAndRegister(BlindLARRequest blindLARRequest);


    // region 工具方法

    /**
     * 通过手机号查询志愿者信息
     * @param phone 志愿者手机号
     * @return 志愿者信息
     */
    Volunteer getVolunteerByPhone(String phone);


    /**
     * 通过手机号查询用户(视障人士)信息
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    Blind getByPhone(String phone);

    /**
     * 用户注册登录返回信息脱敏
     * @param newBlind 返回的盲人信息
     * @param token 登录token
     * @return 脱敏后的对象
     */
    BlindLoginSuccessVO getLoginSuccessVO(Blind newBlind, String token);

    /**
     * 登录成功实现令牌生成与redis储存
     * @param blind 盲人信息
     * @return token
     */
    String loginSuccess(Blind blind);


    // endregion
}
