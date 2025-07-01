package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.user.BlindLoginSuccessVO;
import com.swj.shiwujie.model.domain.Blind;
import com.baomidou.mybatisplus.extension.service.IService;
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
     * @param phone
     * @return
     */
    BlindLoginSuccessVO loginAndRegisterQuickly(String phone);



    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param blindLARRequest
     * @return
     */
    BlindLoginSuccessVO loginAndRegister(BlindLARRequest blindLARRequest);


    // ----------------------------------------------------------

    /**
     * 通过手机号查询用户信息
     * @param phone
     * @return
     */
    Blind getByPhone(String phone);


    /**
     * 用户注册登录返回信息脱敏
     * @param newBlind
     * @return
     */
    BlindLoginSuccessVO getLoginSuccessVO(Blind newBlind, String token);

    /**
     * 登录成功实现令牌生成与redis储存
     * @param blind
     */
    String loginSuccess(Blind blind);
}
