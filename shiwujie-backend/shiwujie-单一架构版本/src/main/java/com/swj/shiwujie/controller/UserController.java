package com.swj.shiwujie.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.UserLoginVO;
import com.swj.shiwujie.model.VO.UserVO;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.request.UserLoginRequest;
import com.swj.shiwujie.model.request.UserRegisterRequest;
import com.swj.shiwujie.service.UserService;
import com.swj.shiwujie.utils.JWTUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.swj.shiwujie.constants.UserConstant.*;

/**
 * 用户管理
 *
 * @author ddc
 */
@RestController
@RequestMapping("/shiwujie/user")
@Slf4j
@CrossOrigin
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 检验jwt令牌是否存在且合法
     */
    @GetMapping("/test/jwt")
    public BaseResponse<UserVO> testJwt(@RequestHeader("Authorization") String token, HttpServletRequest  request) {
        Long currentUserId = userService.getCurrentUserId(request);
        if(currentUserId == null || currentUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN,"用户未登录");
        }
        UserVO res = userService.testJwt(currentUserId,token);

        return ResultUtils.success(res);
    }

    /**
     * 使用手机号与密码登录注册
     *
     * @param userRegisterRequest 用户账号与密码
     * @return 用户的脱敏信息与token
     */

    @PostMapping("/LoginAndRegister")
    public BaseResponse<UserLoginVO> userLoginAndRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        UserLoginVO res = userService.userLoginAndRegister(userRegisterRequest);
        return ResultUtils.success(res);
    }


    /**
     * 使用手机号一键登录
     * @param userPhone 手机号
     * @return 用户的脱敏信息与token
     */
    @PostMapping("/LoginAndRegisterQuickly")
    public BaseResponse<UserLoginVO> userLoginAndRegisterQuickly(String userPhone){
        UserLoginVO res = userService.userLoginAndRegisterQuickly(userPhone);
        if(res == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"登录失败");
        }

        return ResultUtils.success(res);

    }

    /**
     * 用户查看自己的全部信息
     */
    @GetMapping("/mine/check")
    public BaseResponse<UserVO> userList(HttpServletRequest  request) {
        User user = userService.getById(userService.getCurrentUserId(request));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户不存在");
        }

        return ResultUtils.success(this.userService.getUserVOByUser(user));
    }


    /**
     * 删除用户
     * @param request
     * @return
     */
    @GetMapping("/mine/delete")
    public  BaseResponse<UserVO> userDelete(HttpServletRequest  request) {


        UserVO userVO = userService.removeUserById(request);

        //返回删除的用户信息
        return ResultUtils.success(userVO);
    }


    /**
     * 用户信息修改
     * @param user
     * @param request
     * @return
     */
    @PutMapping("/mine/update")
    public BaseResponse<UserVO> userUpdate(@RequestBody User user,HttpServletRequest  request) {
        Long currentUserId = userService.getCurrentUserId(request);
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN,"用户未登录");
        }
        UserVO updateUser = userService.updateUserById(user,currentUserId);
        if (updateUser == null) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户修改失败");
        }
        //返回修改的用户信息
        return ResultUtils.success(updateUser);
    }

    /**
     * 退出登录
     */
    @GetMapping("/mine/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest  request) {
        Long currentUserId = userService.getCurrentUserId(request);
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN,"用户未登录");
        }
        boolean result = userService.userLogout(currentUserId);
        return ResultUtils.success(result);
    }


    /**
     * 校验密码是否正确
     * @param userPassword
     * @param request
     * @return
     */
    @GetMapping("/mine/testpassword")
    public BaseResponse<Boolean> testPassword(String userPassword, HttpServletRequest  request) {
        Long currentUserId = userService.getCurrentUserId(request);
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN,"用户未登录");
        }
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户不存在");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        if(user.getUserPassword().equals(encryptPassword)) {
            return ResultUtils.success(true);
        } else {
            throw new BusinessException(ErrorCode.USER_ERROR, "密码错误");
        }
    }

    /**
     * 添加残疾人证
     * @param certificate 残疾人证
     * @param request 请求
     * @return 脱敏的用户信息
     */
    @PostMapping("/certificate/add")
    public BaseResponse<UserVO> addCertificate(String certificate, HttpServletRequest  request){
        Long currentUserId = userService.getCurrentUserId(request);
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN,"用户未登录");
        }
        if(certificate == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        User user = userService.getById(currentUserId);
        //残疾人证20位,校验
        if(userService.certificateVerify(certificate)){
            user.setUserCertificate(certificate);
            boolean b = userService.updateById(user);
            if(!b){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"操作失败");
            }
        }
        return ResultUtils.success(userService.getUserVOByUser(user));
    }



}