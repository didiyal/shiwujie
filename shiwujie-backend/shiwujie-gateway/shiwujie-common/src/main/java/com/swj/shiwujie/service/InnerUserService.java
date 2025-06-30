package com.swj.shiwujie.service;


import com.swj.shiwujie.model.domain.User;

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
