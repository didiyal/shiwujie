package com.swj.shiwujie.socket.inner;

import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.call.InnerSocket;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * 内部socket实现类
 */
@DubboService
public class InnerSocketImpl implements InnerSocket {

    @Resource
    private CoordinationSocketHandler coordinationSocketHandler;


    @Override
    public void noticeTakePhoto(SocketData socketData) {
        coordinationSocketHandler.noticeTakePhoto(socketData);
    }

    @Override
    public void noticeVideoHelp(SocketData socketData) {
        coordinationSocketHandler.noticeVideoHelp(socketData);

    }

    @Override
    public void noticeUrgentHelp(SocketData socketData) {
        coordinationSocketHandler.noticeUrgentHelp(socketData);

    }

    @Override
    public void noticeJumpSoftware(SocketData socketData) {
        coordinationSocketHandler.noticeJumpSoftware(socketData);

    }

    @Override
    public void noticeJumpToUserUpdate(SocketData socketData) {
        coordinationSocketHandler.noticeJumpToUserUpdate(socketData);

    }
}
