package com.swj.shiwujie.controller;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mcp.EmergencyTokenStore;
import com.swj.shiwujie.model.domain.call.Urgenthelp;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.UrgenthelpService;
import com.swj.shiwujie.service.call.InnerSocket;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


/**
 * 视频求助接口
 */
@RestController
@RequestMapping("/api/call/urgenthelp")
@Slf4j
@Tag(name = "紧急求助接口")
public class UrgenthelpController {


    @Resource
    UrgenthelpService urgenthelpService;

    // chunk-2e-4 gate ③：App 显式确认面消费 token 所需依赖（非-MCP HTTP 端点）
    @Resource
    private InnerSocket innerSocket;

    @Resource
    private EmergencyTokenStore emergencyTokenStore;

    @Resource
    private BlindService blindService;


    // region 视障人士操作

    /**
     * 视障人士发起求助
     *
     * @return 加入是否成功
     */
    @GetMapping("/blind/add")
    @Operation(summary = "视障人士发起紧急求助")
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
    @Operation(summary = "视障人士取消紧急求助")
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
    @Operation(summary = "上传紧急求助视频")
    public BaseResponse<Boolean> blindUpdateVideoPath(String videoPath, HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginBlindId = LoginUtils.getLoginBlindId(request);


        Urgenthelp urgenthelp = urgenthelpService.getByBlindId(loginBlindId);
        ThrowUtils.throwIf(ObjUtil.isNull(urgenthelp), ErrorCode.PARAMS_ERROR, "未找到进行中的紧急求助");
        urgenthelp.setVideoPath(videoPath);

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
    @Operation(summary = "家属加入紧急求助")
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
    @Operation(summary = "挂断紧急求助视频")
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

        urgenthelp.setEndTime(DateUtil.date());
        urgenthelp.setHelpStatus(CallHelpStatusEnum.END_HELP.getHelpStatus());
        long between = DateUtil.between(urgenthelp.getResponseTime(), urgenthelp.getEndTime(), DateUnit.MINUTE);
        urgenthelp.setDuration(between);
        boolean b = urgenthelpService.updateById(urgenthelp);
        ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }


    // region chunk-2e-4 gate ③：App 显式确认面消费 token

    /**
     * 盲人 App 显式确认紧急求助（gate ③ 消费 token → 推 WS 5003 通知所有家属）。
     *
     * <p>非-MCP HTTP 端点——agent 无此路径，红队 Q18 第三道门（盲人单声道无视觉冗余，须屏幕硬确认）。
     * 镜像 {@code SignalMcpTools.requestEmergencyHelpConfirm} 成功路径，但 token 经
     * {@link EmergencyTokenStore#consumeByApp} 消费（人工确认超越轮次闸，不做 same-turn 检查）。</p>
     *
     * @param token   prepare() 经 114 帧下发、App 回传的确认码
     */
    @PostMapping("/blind/confirm")
    @Operation(summary = "盲人确认紧急求助（gate ③ 消费 token）")
    public BaseResponse<Boolean> blindConfirmUrgenthelp(@RequestParam String token, HttpServletRequest request) {
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginBlindId), ErrorCode.PARAMS_ERROR, "只有视障人士才可以操作");
        ThrowUtils.throwIf(StrUtil.isBlank(token), ErrorCode.PARAMS_ERROR, "确认码不能为空");

        EmergencyTokenStore.VerifyResult vr = emergencyTokenStore.consumeByApp(token, loginBlindId);
        ThrowUtils.throwIf(!vr.ok(), ErrorCode.PARAMS_ERROR, vr.message());

        // token 消费通过 → 推 WS 5003（镜像 confirm() MCP：盲人 phone 查 session 推信令）
        Blind blind = blindService.getById(loginBlindId);
        ThrowUtils.throwIf(blind == null || StrUtil.isBlank(blind.getPhone()), ErrorCode.PARAMS_ERROR, "用户信息异常");
        SocketData d = new SocketData();
        d.setBlindPhone(blind.getPhone());
        d.setRequestType(5003);
        innerSocket.noticeUrgentHelp(d);
        log.info("gate ③ 紧急确认通过 blindId={}，已推 5003", loginBlindId);
        return ResultUtils.success(true);
    }

    // endregion


}
