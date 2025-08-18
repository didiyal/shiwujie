package com.swj.shiwujie.socket;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.call.SocketVO;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.call.SocketData;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Configuration
@ChannelHandler.Sharable
@Slf4j
public class CoordinationSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static Map<String, Channel> cmap = new HashMap<>();

    public static Map<Channel, String> pmap = new HashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端建立连接，通道开启！");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String phone = pmap.get(channel);
        pmap.remove(channel);
        cmap.remove(phone);
        System.out.println(phone+"与客户端断开连接，通道关闭！");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        //接收的消息
        SocketData socketData = JSONUtil.toBean(msg.text(), SocketData.class);
        Integer type = socketData.getRequestType();
        switch (type) {
            case -1:   // ping
                ping(socketData);
                break;
            case 0:   // 建立连接
                websocketLogin(socketData, ctx);
                break;
            case 2: // 志愿者初始化成功
                toBlindJoin(socketData);
                break;
        }
        ctx.channel().writeAndFlush(new TextWebSocketFrame("收到客户端的数据"));
        System.out.println(String.format("收到客户端%s的数据：%s", pmap.get(ctx.channel()), msg.text()));
    }


    // region 初始化与心跳

    /**
     * ping - -1
     * @param socketData socketData
     */
    private void ping(SocketData socketData) {
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "pong", -1, socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info( socketData.getBlindPhone() + "心跳 - -1" );
        }else{
            Channel channel = cmap.get(socketData.getVolunteerPhone());
            String response = this.getResponse(0, "pong", -1, socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info( socketData.getVolunteerPhone() + "心跳 - -1" );
        }

    }



    /**
     * 初始化socket登录 - 0
     * 返回type,0
     *
     * @param socketData socketData
     * @param ctx         ChannelHandlerContext
     */
    private void websocketLogin(SocketData socketData, ChannelHandlerContext ctx) {
        String phone = socketData.getVolunteerPhone();
        if (StrUtil.isBlankIfStr(phone)) {
            phone = socketData.getBlindPhone();
        }
        cmap.put(phone, ctx.channel());
        pmap.put(ctx.channel(), phone);
        System.out.println(phone + "登录");

        String response = this.getResponse(0, "初始化成功", 0, socketData);
        log.info("初始化socket登录 - 0" );
        ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
    }

    // endregion


    // region 视频通话

    /**
     * 匹配成功,向志愿者发送信息 1
     *
     * @param socketData socketData
     */
    public void matchSuccess(SocketData socketData) {

        if (cmap.containsKey(socketData.getVolunteerPhone())) {
            Channel channel = cmap.get(socketData.getVolunteerPhone());

            String response = this.getResponse(0, "匹配成功", 1, socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("匹配成功,向志愿者发送信息 - 1" );
        }else{
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该用户未连接服务器");
        }
    }



    /**
     * 志愿者初始化成功,向盲人转发 2
     * @param socketData socketData
     */
    private void toBlindJoin(SocketData socketData) {
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "志愿者视频初始化成功", 2, socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
        }else{
            Channel channel = cmap.get(socketData.getVolunteerPhone());
            String response = this.getResponse(1, "系统错误", 2, socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
        }
        log.info("志愿者初始化成功,向盲人转发 - 2" );
    }


    /**
     * 盲人向家属紧急求助,向家属转发 3
     * @param volunteerList 家属列表
     * @param socketData 返回类型
     */
    public void urgenthelpToFamily(List<Volunteer> volunteerList,SocketData socketData) {

        for (Volunteer volunteer : volunteerList) {
            String phone = volunteer.getPhone();
            if(ObjUtil.isNotNull(phone)){
                if(cmap.containsKey(phone)){
                    Channel channel = cmap.get(phone);
                    socketData.setVolunteerPhone(phone);
                    socketData.setChannelId(volunteer.getVolunteerId());
                    String response = this.getResponse(0, "紧急求助", 3, socketData);
                    channel.writeAndFlush(new TextWebSocketFrame(response));
                }
            }
        }
        log.info("盲人向家属紧急求助,向家属转发 - 3" );

    }


    /**
     * 盲人取消求助通知 4
     * @param volunteerList 家属列表
     * @param socketData 返回类型
     */
    public void cancelUrgenthelp(List<Volunteer> volunteerList,SocketData socketData) {

        for (Volunteer volunteer : volunteerList) {
            String phone = volunteer.getPhone();
            if(ObjUtil.isNotNull(phone)){
                if(cmap.containsKey(phone)){
                    Channel channel = cmap.get(phone);
                    socketData.setVolunteerPhone(phone);
                    socketData.setChannelId(volunteer.getVolunteerId());
                    String response = this.getResponse(0, "家属取消紧急求助", 4, socketData);
                    channel.writeAndFlush(new TextWebSocketFrame(response));
                }
            }
        }


        log.info("盲人取消求助通知 - 4" );
    }

    // endregion


    //region AI与前端联动


    /**
     * 通知前端拍照识别 5001
     */
    public void noticeTakePhoto(SocketData socketData){
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "拍照识别", socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("通知前端拍照识别 - 5001" );
        }else{
            this.getResponse(1, "系统错误", socketData);
        }
    }


    /**
     * 通知前端视频求助 5002
     */
    public void noticeVideoHelp(SocketData socketData){
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "视频求助", socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("通知前端视频求助 - 5002" );
        }else{
            this.getResponse(1, "系统错误", socketData);
        }

    }


    /**
     * 通知前端紧急求助 5003
     */
    public void noticeUrgentHelp(SocketData socketData){
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "紧急求助", socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("通知前端紧急求助 - 5003" );
        }else{
            this.getResponse(1, "系统错误", socketData);
        }

    }


    /**
     * 通知前端跳转软件 5004
     */
    public void noticeJumpSoftware(SocketData socketData){
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "跳转软件", socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("通知前端跳转软件 - 5004" );
        }else{
            this.getResponse(1, "系统错误", socketData);
        }

    }


    /**
     * 通知前端跳转到用户修改页面 5005
     */
    public void noticeJumpToUserUpdate(SocketData socketData){
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "跳转到用户修改页面", socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("通知前端跳转到用户修改页面 - 5005" );
        }else{
            this.getResponse(1, "系统错误", socketData);
        }
    }

    /**
     * 通知前端导航 5006
     */
    public void noticeNavigation(SocketData socketData){
        if (cmap.containsKey(socketData.getBlindPhone())) {
            Channel channel = cmap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "开启导航", socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
            log.info("通知前端开启导航 - 5006" );
        }else{
            this.getResponse(1, "系统错误", socketData);
        }

    }

    //endregion





    private String getResponse(Integer code, String message, Integer requestType, SocketData socketData) {
        SocketVO socketVO = new SocketVO();
        socketVO.setSocketData(socketData);
        socketVO.setCode(code);
        socketVO.setMessage(message);
        socketData.setRequestType(requestType);
        socketVO.setSocketData(socketData);
        return JSONUtil.toJsonStr(socketVO);
    }


    private String getResponse(Integer code, String message, SocketData socketData) {
        SocketVO socketVO = new SocketVO();
        socketVO.setSocketData(socketData);
        socketVO.setCode(code);
        socketVO.setMessage(message);
        return JSONUtil.toJsonStr(socketVO);
    }



}