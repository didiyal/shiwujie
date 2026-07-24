package com.swj.shiwujie.socket.inner;

import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.call.InnerSocket;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 内部socket实现类
 */
@Slf4j
@Service
public class InnerSocketImpl implements InnerSocket {

    @Resource
    private CoordinationSocketHandler coordinationSocketHandler;


    @Override
    public void noticeTakePhoto(SocketData socketData) {
        log.info("内部socket - 拍照识别");
        coordinationSocketHandler.noticeTakePhoto(socketData);
    }

    @Override
    public void noticeVideoHelp(SocketData socketData) {
        log.info("内部socket - 视频求助");
        coordinationSocketHandler.noticeVideoHelp(socketData);

    }

    @Override
    public void noticeUrgentHelp(SocketData socketData) {
        log.info("内部socket - 紧急求助");
        coordinationSocketHandler.noticeUrgentHelp(socketData);

    }

    @Override
    public void noticeEmergencyToken(SocketData socketData) {
        log.info("内部socket - 紧急求助确认 token（gate ③ 114）");
        coordinationSocketHandler.noticeEmergencyToken(socketData);
    }

    @Override
    public void noticeJumpSoftware(SocketData socketData) {
        log.info("内部socket - 跳转软件");
        coordinationSocketHandler.noticeJumpSoftware(socketData);

    }

    @Override
    public void noticeJumpToUserUpdate(SocketData socketData) {
        log.info("内部socket - 跳转到用户修改页面:"+socketData.getVolunteerPhone());
        coordinationSocketHandler.noticeJumpToUserUpdate(socketData);

    }


    @Override
    public void noticeNavigation(SocketData socketData) {
        log.info("内部socket - 导航:" + socketData.getVolunteerPhone());
        coordinationSocketHandler.noticeNavigation(socketData);

    }
}
