package com.swj.shiwujie.controller;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.call.Videohelp;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.service.VideohelpService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Time;
import java.util.List;


/**
 * 视频求助接口
 */
@RestController
@RequestMapping("/videohelp")
@Slf4j
@Api(tags = "视频求助接口")
public class VideohelpController {


    @Resource
    VideohelpService videohelpService;


    // region 志愿者操作

    /**
     * 志愿者加入匹配
     *
     * @return 加入是否成功
     */
    @GetMapping("/volunteer/add")
    @ApiOperation("志愿者加入视频求助匹配")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> volunteerCreateVideohelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.PARAMS_ERROR, "只有志愿者才可以操作");

        boolean b = videohelpService.createVideohelp(loginVolunteerId, loginUserPhone);


        return ResultUtils.success(b);
    }


    /**
     * 志愿者退出匹配
     *
     * @return 是否成功
     */
    @DeleteMapping("/volunteer/delete")
    @ApiOperation("志愿者退出视频求助匹配")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> volunteerLeaveVideohelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");


        boolean b = videohelpService.removeVolunteerFromVideohelp(loginVolunteerId, loginUserPhone);


        return ResultUtils.success(b);
    }


    // endregion


    //region 视障人士操作

    /**
     * 视障人士加入匹配
     *
     * @return 是否成功
     */

    @GetMapping("/blind/join")
    @ApiOperation("视障人士加入视频求助匹配")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> blindJoinVideohelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);

        boolean result = videohelpService.joinVideohelp(loginBlindId,loginUserPhone);

        return ResultUtils.success(result);

    }

    /**
     * 上传视频
     *
     * @param videoPath 视频路径
     * @param request     请求
     * @return 是否成功
     */
    @PostMapping("/join")
    @ApiOperation("上传视频求助视频")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> blindUpdateVideoPath(String videoPath, HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginBlindId = LoginUtils.getLoginBlindId(request);


        Videohelp videohelp = videohelpService.getByBlindId(loginBlindId);
        videohelp.setVideo_path(videoPath);

        //3.处理
        Boolean b = videohelpService.updateById(videohelp);

        return ResultUtils.success(b);

    }


    //endregion


    /**
     * 挂断视频
     *
     * @param request 请求
     * @return 是否成功
     */
    @DeleteMapping("/delete/leave")
    @ApiOperation("挂断视频求助通话")
    public BaseResponse<Boolean> hangupVideohelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        List<Videohelp> videohelps = null;
        if(ObjUtil.isNotNull(loginVolunteerId)){
            videohelps = videohelpService.getHelpingByVolunteerId(loginVolunteerId);
        } else if (ObjUtil.isNotNull(loginBlindId)) {
            videohelps = videohelpService.getHelpingByBlindId(loginBlindId);
        }
        // 只有通话中才可以挂断通话
        ThrowUtils.throwIf(ObjUtil.isNull(videohelps),ErrorCode.PARAMS_ERROR,"只有通话中才可以挂断通话");

        for (Videohelp videohelp : videohelps) {
            videohelp.setEnd_time(DateUtil.date());
            videohelp.setHelp_status(CallHelpStatusEnum.END_HELP.getHelpStatus());
            long between = DateUtil.between(videohelp.getResponse_time(), videohelp.getEnd_time(), DateUnit.MINUTE);
            videohelp.setDuration(between);
        }
        videohelpService.updateBatchById(videohelps);

        return ResultUtils.success(true);
    }

}
