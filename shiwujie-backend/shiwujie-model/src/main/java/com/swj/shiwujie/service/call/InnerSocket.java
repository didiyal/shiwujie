package com.swj.shiwujie.service.call;


import com.swj.shiwujie.model.request.call.SocketData;

/**
 * 内部Socket服务接口
 */
public interface InnerSocket {


    /**
     * 通知前端拍照识别
     */
    void noticeTakePhoto(SocketData socketData);


    /**
     * 通知前端视频求助
     */
    void noticeVideoHelp(SocketData socketData);


    /**
     * 通知前端紧急求助
     */
    void noticeUrgentHelp(SocketData socketData);


    /**
     * 通知前端跳转软件
     */
    void noticeJumpSoftware(SocketData socketData);


    /**
     * 通知前端跳转到用户修改页面
     */
    void noticeJumpToUserUpdate(SocketData socketData);


    /**
     * 通知前端进行导航
     */
    void noticeNavigation(SocketData socketData);


}
