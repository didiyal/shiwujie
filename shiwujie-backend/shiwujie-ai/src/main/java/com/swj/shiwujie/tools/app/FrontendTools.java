package com.swj.shiwujie.tools.app;


import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.call.InnerSocket;
import com.swj.shiwujie.utils.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 前端调用工具类
 */

@Component
@Slf4j
public class FrontendTools {

    @DubboReference
    private InnerSocket innerSocket;


    /**
     * 通知前端进行拍照识别
     */
    @Tool(name = "Take photo for recognition",
            description = "Help users take photos for recognition, results will be returned directly to the user")
    public String noticeTakePhoto() {
        log.info("开始拍照识别");
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5001);
        innerSocket.noticeTakePhoto(socketData);
        return "已开始拍照识别";
    }


    /**
     * 通知前端进行视频求助
     */
    @Tool(name = "Request video assistance",
            description = "Help users connect with volunteers for video assistance, results will be presented directly to the user")
    public String noticeVideoHelp() {
        log.info("开始视频求助");
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5002);
        innerSocket.noticeVideoHelp(socketData);
        return "已开始视频求助";
    }


    /**
     * 通知前端进行紧急求助
     */
    @Tool(name = "Request emergency assistance",
            description = "Family emergency assistance, will establish a family video call for help, results will be presented directly to the user")
    public String noticeUrgentHelp() {
        log.info("开始紧急求助");
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5003);
        innerSocket.noticeUrgentHelp(socketData);
        return "已开始紧急求助";
    }


    /**
     * 通知前端进行跳转到其它软件
     */
    @Tool(name = "Jump to application",
            description = "Jump to other software such as WeChat, phone, messages, etc., results will be presented directly to the user")
    public String noticeJumpSoftware(@ToolParam(description = "Name of the application to jump to") String appName) {
        log.info("开始跳转到其它软件");
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5004);
        // 这里短暂用这个代替
        socketData.setVolunteerPhone(appName);
        innerSocket.noticeJumpSoftware(socketData);
        return "已开始跳转到" + appName;
    }


    /**
     * 通知前端进行导航
     *
     * @param destination 目标
     */
    @Tool(name = "Navigate to location",
            description = "Navigate to the specified location, results will be presented directly to the user")
    public String noticeNavigation(@ToolParam(description = "Destination location") String destination) {
        log.info("开始导航");
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5006);
        // 这里短暂用这个代替
        socketData.setVolunteerPhone(destination);
        innerSocket.noticeNavigation(socketData);
        return "已开始导航至" + destination;
    }
}