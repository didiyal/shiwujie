package com.swj.shiwujie.controller;


import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.VideoVO;
import com.swj.shiwujie.service.InnerUserService;
import com.swj.shiwujie.service.VideoService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户频道
 */
@RestController
@RequestMapping("/channel")
@Slf4j
public class VideoController {

    @DubboReference
    private InnerUserService innerUserService;

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
        Long currentUserId = LoginUtils.getCurrentUserId(request);

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
        Long currentUserId = LoginUtils.getCurrentUserId(request);

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
        //获取登录用户信息
        Long currentUserId = LoginUtils.getCurrentUserId(request);

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
        //获取登录用户信息
        Long currentUserId = LoginUtils.getCurrentUserId(request);

        Boolean result =  videoService.leaveBlindChannelByUserId(currentUserId);

        return ResultUtils.success(result);
    }
}
