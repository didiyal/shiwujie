package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.UserLoginVO;
import com.swj.shiwujie.model.VO.UserVO;
import com.swj.shiwujie.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.UserLoginRequest;
import com.swj.shiwujie.model.request.UserRegisterRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ldl
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2024-12-15 23:26:31
 */

public interface UserService extends IService<User> {


    boolean emailVerify(String userEmail);

    boolean passwordVerify(String userPassword);

    boolean phoneVerify(String userPhone);



    /**
     * 获取当前登录用户id
     * @param request
     * @return
     */
    Long getCurrentUserId(HttpServletRequest request);

    /**
     * 获取用户信息
     * 通过请求
     * @param request
     */
    User getUserByRequest(HttpServletRequest request);


    /**
     * 注销用户
     * @param request
     * @return
     */
    UserVO removeUserById(HttpServletRequest request);


    /**
     * 用户信息修改
     * @param user
     * @param currentUserId
     * @return
     */
    UserVO updateUserById(User user, Long currentUserId);

    /**
     * 退出登录
     * @param currentUserId
     * @return
     */
    boolean userLogout(Long currentUserId);


    /**
     * 用户信息统一返回脱敏
     * @param user
     * @return
     */
    UserVO getUserVOByUser(User user);

    /**
     * 通过用户的channel获取id
     * @param channel
     * @return
     */
    User getByChannel(String channel);

    /**
     * 用户一键登录注册
     * @param userPhone 用户手机号
     * @return 用户的脱敏信息与token
     */
    UserLoginVO userLoginAndRegisterQuickly(String userPhone);

    /**
     * 用户账号密码一键登录注册
     * @param userRegisterRequest 用户的手机号与密码
     * @return 用户的脱敏信息与token
     */
    UserLoginVO userLoginAndRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 校验残疾人证是否合法
     * @param certificate
     * @return
     */
    boolean certificateVerify(String certificate);


    /**
     * 将用户登录信息传入redis
     * @param id
     * @return
     */
    boolean setUserToRedis(Long id,String token);

    /**
     * 生成随机的用户账号
     * @return
     */
    String generateUserAccount();

    /**
     * 登录的用户信息脱敏
     * @param user
     * @return
     */
    UserLoginVO getUserLoginVOByUser(User user);

    /**
     * 校验令牌
     * @param currentUserId
     * @param token
     * @return
     */
    UserVO testJwt(Long currentUserId, String token);

    /**
     * 通过通话频道获取正在通话的双方信息
     * @param channel
     * @return
     */
    List<User> getUsersByChannel(String channel);


    /**
     * 挂断通话修改双方的通话信息
     * @param channel
     * @return
     */
    boolean updateCallUsersInformation(String channel);



}
