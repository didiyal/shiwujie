package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.user.blind.BlindLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.CommunityJoinRequest;
import com.swj.shiwujie.model.request.user.blind.BlindLARRequest;
import com.swj.shiwujie.model.request.user.blind.BlindUpdatePasswordRequest;

import java.util.List;

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


    /**
     * 修改密码
     *
     * @param blindUpdatePassword 原密码与要修改的密码
     * @return 是否成功
     */
    boolean updateBlindPassword(BlindUpdatePasswordRequest blindUpdatePassword);


    /**
     * 更新用户
     * 修改用户名,性别,身份证号,残疾人证
     * 后期可以修改经纬度与位置信息
     * @param blind 用户更新信息
     * @return 脱敏后的用户信息
     */
    boolean updateBlind(Blind blind);

    /**
     * 分页查询社区视障人士
     * @param communityId 社区ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页视障人士VO列表
     */
    Page<BlindVO> pageQueryByCommunityId(Long communityId, long current, long size);

    /**
     * 加入社区
     * @param blindId 视障人士ID
     * @param request 加入社区请求
     * @return 是否成功
     */
    boolean joinCommunity(Long blindId, CommunityJoinRequest request);

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
    String generateLoginToken(Blind blind);


    /**
     * 用户信息脱敏(不含token)
     * @param blind 盲人信息
     * @return 脱敏后的信息
     */
    BlindVO getBlindVO(Blind blind);


    /**
     * 校验密码格式是否正确
     * @param password 密码
     * @return 是否正确
     */
    boolean validatePassword(String password);


    /**
     * 通过家庭id获取成员信息
     * @param familyId 家庭id
     * @return 盲人信息
     */
    List<BlindVO> getBlindListByFamilyId(Long familyId);


    // endregion
}
