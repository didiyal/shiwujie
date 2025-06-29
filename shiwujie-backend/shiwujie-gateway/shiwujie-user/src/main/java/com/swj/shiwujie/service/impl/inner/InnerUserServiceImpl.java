package com.swj.shiwujie.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.enums.UserCallStatusEnum;
import com.swj.shiwujie.service.InnerUserService;
import com.swj.shiwujie.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;



    @Override
    public boolean updateById(User user) {
        return userService.updateById(user);
    }

    @Override
    public User getById(Long id) {
        return userService.getById(id);
    }

    /**
     * 通过用户的通话频道channel获取用户信息
     *
     * @param channel 通话频道
     * @return
     */
    @Override
    public User getByChannel(String channel) {
        if(channel == null || "".equals(channel)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户没有建立视频通话请求");
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("callChannel",channel);
        User user = userService.getOne(userQueryWrapper);
        if(user == null){
            throw new BusinessException(ErrorCode.USER_ERROR,"用户没有建立视频通话请求");
        }
        return user;
    }


    /**
     * 挂断通话修改双方的通话信息
     *
     * @param channel
     * @return
     */
    @Override
    public boolean updateCallUsersInformation(String channel) {
        //修改盲人与志愿者的信息
        List<User> users = userService.getUsersByChannel(channel);
        if(users != null){
            users.forEach(user1 -> {
                user1.setCallStatus(UserCallStatusEnum.NO_CALLING.getValue());
                user1.setCallChannel("");
            });
            userService.saveOrUpdateBatch(users);
        }
        return true;
    }
}
