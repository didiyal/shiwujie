package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.CommunityJoinRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePasswordRequest;

import java.util.List;

/**
 * @author Administrator
 * @description 针对表【Volunteer(视障人士信息表)】的数据库操作Service
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


    /**
     * 修改密码
     *
     * @param volunteerUpdatePassword 原密码与要修改的密码
     * @return 是否成功
     */
    boolean updateVolunteerPassword(VolunteerUpdatePasswordRequest volunteerUpdatePassword);


    /**
     * 更新用户
     * 修改用户名,性别,身份证号,残疾人证
     * 后期可以修改经纬度与位置信息
     * @param volunteer 用户更新信息
     * @return 脱敏后的用户信息
     */
    boolean updateVolunteer(Volunteer volunteer);


    /**
     * 删除志愿者(同时删除创建的家庭)
     * @param volunteerId 志愿者id
     * @param loginUserPhone 登录手机号
     * @return 是否成功
     */
    boolean deleteVolunteer(Long volunteerId, String loginUserPhone);




    // region 工具方法

    /**
     * 通过手机号查询盲人信息
     * @param phone 盲人手机号
     * @return 盲人信息
     */
    Blind getBlindByPhone(String phone);


    /**
     * 通过手机号查询用户(视障人士)信息
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    Volunteer getByPhone(String phone);

    /**
     * 用户注册登录返回信息脱敏
     * @param newVolunteer 返回的盲人信息
     * @param token 登录token
     * @return 脱敏后的对象
     */
    VolunteerLoginSuccessVO getLoginSuccessVO(Volunteer newVolunteer, String token);

    /**
     * 分页查询社区下的志愿者
     * @param communityId 社区ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页志愿者VO列表
     */
    Page<VolunteerVO> pageQueryByCommunityId(Long communityId, long current, long size);

    /**
     * 加入社区
     * @param volunteerId 志愿者ID
     * @param request 加入社区请求
     * @return 是否成功
     */
    boolean joinCommunity(Long volunteerId, CommunityJoinRequest request);

    /**
     * 登录成功实现令牌生成与redis储存
     * @param volunteer 盲人信息
     * @return token
     */
    String generateLoginToken(Volunteer volunteer);


    /**
     * 用户信息脱敏(不含token)
     * @param volunteer 盲人信息
     * @return 脱敏后的信息
     */
    VolunteerVO getVolunteerVO(Volunteer volunteer);

    /**
     * 校验密码格式是否正确
     * @param password 密码
     * @return 是否正确
     */
    boolean validatePassword(String password);


    /**
     * 通过家庭id获取成员信息
     * @param familyId 家庭id
     * @return 志愿者信息
     */
    List<VolunteerVO> getVolunteerVOListByFamilyId(Long familyId);




    // endregion
}
