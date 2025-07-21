package com.swj.shiwujie.controller;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.call.Urgenthelp;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.service.UrgenthelpService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 视频求助接口
 */
@RestController
@RequestMapping("/urgenthelp")
@Slf4j
public class UrgenthelpController {


    @Resource
    UrgenthelpService urgenthelpService;


    // region 视障人士操作

    /**
     * 视障人士发起求助
     *
     * @return 加入是否成功
     */
    @GetMapping("/blind/add")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> blindCreateUrgenthelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginBlindId), ErrorCode.PARAMS_ERROR, "只有视障人士才可以操作");

        boolean b = urgenthelpService.createUrgenthelp(loginBlindId, loginUserPhone);


        return ResultUtils.success(b);
    }




    /**
     * 视障人士取消求助
     *
     * @return 是否成功
     */
    @DeleteMapping("/blind/delete")
    public BaseResponse<Boolean> blindLeaveUrgenthelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginBlindId), ErrorCode.PARAMS_ERROR, "只有视障人士才可以操作");


        boolean b = urgenthelpService.removeFromUrgenthelp(loginBlindId, loginUserPhone);


        return ResultUtils.success(b);
    }




    /**
     * 上传视频
     *
     * @param videoPath 视频路径
     * @param request     请求
     * @return 是否成功
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> blindUpdateVideoPath(String videoPath, HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginBlindId = LoginUtils.getLoginBlindId(request);


        Urgenthelp urgenthelp = urgenthelpService.getByBlindId(loginBlindId);
        urgenthelp.setVideo_path(videoPath);

        //3.处理
        Boolean b = urgenthelpService.updateById(urgenthelp);

        return ResultUtils.success(b);

    }



    // endregion

    /**
     * 家属加入求助
     * @param blindPhone 求助盲人id
     * @return 是否成功
     */

    @GetMapping("/volunteer/join")
    public BaseResponse<Boolean> familyJoinUrgenthelp(String blindPhone,HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);

        boolean result = urgenthelpService.joinUrgenthelp(blindPhone,loginVolunteerId,loginUserPhone);

        return ResultUtils.success(result);

    }





    /**
     * 挂断视频
     *
     * @param request 请求
     * @return 是否成功
     */
    @DeleteMapping("/delete/leave")
    public BaseResponse<Boolean> hangupUrgenthelp(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        Urgenthelp urgenthelp = null;
        if(ObjUtil.isNotNull(loginVolunteerId)){
            urgenthelp = urgenthelpService.getHelpingByVolunteerId(loginVolunteerId);
        } else if (ObjUtil.isNotNull(loginBlindId)) {
            urgenthelp = urgenthelpService.getHelpingByBlindId(loginBlindId);
        }
        // 只有通话中才可以挂断通话
        ThrowUtils.throwIf(ObjUtil.isNull(urgenthelp),ErrorCode.PARAMS_ERROR,"只有通话中才可以挂断通话");

        urgenthelp.setEnd_time(DateUtil.date());
        urgenthelp.setHelp_status(CallHelpStatusEnum.END_HELP.getHelpStatus());
        long between = DateUtil.between(urgenthelp.getResponse_time(), urgenthelp.getEnd_time(), DateUnit.MINUTE);
        urgenthelp.setDuration(between);
        boolean b = urgenthelpService.updateById(urgenthelp);
        ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }

}
