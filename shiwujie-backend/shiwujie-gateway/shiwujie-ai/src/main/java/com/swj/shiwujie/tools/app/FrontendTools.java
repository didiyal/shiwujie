package com.swj.shiwujie.tools.app;


import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.call.InnerSocket;
import com.swj.shiwujie.utils.LoginUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 前端调用工具类
 */

@Component
public class FrontendTools {

    @DubboReference
    private InnerSocket innerSocket;


    /**
     * 通知前端进行拍照识别
     */
    public void noticeTakePhoto() {
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5001);
        innerSocket.noticeTakePhoto(socketData);
    }


    /**
     * 通知前端进行视频求助
     */
    public void noticeVideoHelp() {
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5002);
        innerSocket.noticeVideoHelp(socketData);

    }


    /**
     * 通知前端进行紧急求助

     */
    public void noticeUrgentHelp() {
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5003);
        innerSocket.noticeUrgentHelp(socketData);

    }


    /**
     * 通知前端进行跳转到其它软件

     */
    public void noticeJumpSoftware(String appName) {
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5004);
        // 这里短暂用这个代替
        socketData.setVolunteerPhone(appName);
        innerSocket.noticeJumpSoftware(socketData);

    }


    /**
     * 通知前端进行跳转到用户更新页面

     */
    public void noticeJumpToUserUpdate() {
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5005);
        innerSocket.noticeJumpToUserUpdate(socketData);

    }

    /**
     * 通知前端进行导航
     * @param destination 目标
     */
    public void noticeNavigation(String destination) {
        String loginUserPhone = LoginUtils.getLoginUserPhone();
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(loginUserPhone);
        socketData.setRequestType(5006);
        // 这里短暂用这个代替
        socketData.setVolunteerPhone(destination);
        innerSocket.noticeNavigation(socketData);
    }
}
