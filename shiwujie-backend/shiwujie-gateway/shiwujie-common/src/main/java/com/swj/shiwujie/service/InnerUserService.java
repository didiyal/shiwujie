package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.UserLoginVO;
import com.swj.shiwujie.model.VO.UserVO;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.request.UserRegisterRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ldl
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2024-12-15 23:26:31
 */

public interface InnerUserService{



    /**
     * 用户信息修改
     * @param user
     * @return
     */
    boolean updateById(User user);

    /**
     * 通过用户的channel获取id
     * @param id
     * @return
     */
    User getById(Long id);


    /**
     * 通过用户的channel获取id
     * @param channel
     * @return
     */
    User getByChannel(String channel);


    /**
     * 挂断通话修改双方的通话信息
     * @param channel
     * @return
     */
    boolean updateCallUsersInformation(String channel);



}
