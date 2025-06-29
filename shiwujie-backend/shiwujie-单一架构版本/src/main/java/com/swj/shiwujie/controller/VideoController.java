package com.swj.shiwujie.controller;


import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.VideoVO;
import com.swj.shiwujie.model.domain.Video;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.service.VideoService;
import com.swj.shiwujie.service.UserService;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 用户频道
 */
@RestController
@RequestMapping("shiwujie/channel")
@Slf4j
public class VideoController {

    @Resource
    private UserService userService;

    @Resource
    private VideoService videoService;


    /**
     * 获取志愿者通道
     * 待优化
     * @param request
     * @return
     */
    @GetMapping("/getVolunteerChannelAndUid")
    public BaseResponse<VideoVO> getVolunteerChannelAndUid(HttpServletRequest request){
        //获取登录用户信息
        Long currentUserId = userService.getCurrentUserId(request);
        if(currentUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }

        VideoVO result =  videoService.getVolunteerChannelByUserId(currentUserId);

        return ResultUtils.success(result);
    }

    /**
     * 获取盲人通道
     * 待优化
     * @param request
     * @return
     */
    @GetMapping("/getBlindChannelAndUid")
    public BaseResponse<VideoVO> getBlindChannelAndUid(HttpServletRequest request){
        //获取登录用户信息
        Long currentUserId = userService.getCurrentUserId(request);
        if(currentUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }

        VideoVO result =  videoService.getBlindChannelByUserId(currentUserId);

        return ResultUtils.success(result);
    }

    /**
     * 志愿者挂断通话,更新状态
     * @param request
     * @return
     */
    @GetMapping("/leaveVolunteerChannel")
    public BaseResponse<Boolean> leaveVolunteerVideo(HttpServletRequest request){
        Long currentUserId = userService.getCurrentUserId(request);
        if(currentUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }

        Boolean result =  videoService.leaveVolunteerChannelByUserId(currentUserId);

        return ResultUtils.success(result);
    }

    /**
     * 盲人挂断通话,更新状态
     * @param request
     * @return
     */
    @GetMapping("/leaveBlindChannel")
    public BaseResponse<Boolean> leaveBlindVideo(HttpServletRequest request){
        Long currentUserId = userService.getCurrentUserId(request);
        if(currentUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }

        Boolean result =  videoService.leaveBlindChannelByUserId(currentUserId);

        return ResultUtils.success(result);
    }
}
