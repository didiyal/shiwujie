package com.swj.shiwujie.controller;


import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.CallHelpVO;
import com.swj.shiwujie.model.request.CallHelpJoinRequest;
import com.swj.shiwujie.service.CallHelpService;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/shiwujie/callHelp")
public class CallHelpController {

    @Resource
    private CallHelpService callHelpService;



    /**
     * 盲人紧急求助
     * 获取盲人通道
     * 待优化
     * @param request
     * @return
     */
    @GetMapping("/getBlindChannelAndUid")
    public BaseResponse<CallHelpVO> getCallHelpBlindChannelAndUid(HttpServletRequest request){

        CallHelpVO result =  callHelpService.getCallHelpBlindChannel(request);
        return ResultUtils.success(result);
    }




    /**
     * 加入求助的通话
     * 待优化
     * @param
     * @return
     */
    @PostMapping("/joinBlindChannelAndUid")
    public BaseResponse<CallHelpVO> joinCallHelpBlindChannelAndUid(@RequestBody CallHelpJoinRequest callHelpJoinRequest, HttpServletRequest request){

        CallHelpVO result =  callHelpService.joinCallHelpBlindChannel(callHelpJoinRequest,request);

        return ResultUtils.success(result);
    }



    /**
     * 求助的盲人挂断通话
     * ,更新双方状态
     * @param request
     * @return
     */
    @GetMapping("/leaveCallHelpByBlind")
    public BaseResponse<Boolean> leaveVolunteerVideo(HttpServletRequest request){

        Boolean result =  callHelpService.leaveCallHelpByBlind(request);

        return ResultUtils.success(result);
    }

    /**
     * 协助家属挂断通话
     * 更新状态
     * @param request
     * @return
     */
    @GetMapping("/leaveBlindChannel")
    public BaseResponse<Boolean> leaveBlindVideo(HttpServletRequest request){


        Boolean result =  callHelpService.leaveCallHelpByHelpUser(request);

        return ResultUtils.success(result);
    }

}
