package com.swj.shiwujie.socket;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Configuration
@ChannelHandler.Sharable
public class CoordinationSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public Map<String, Channel> cmap = new HashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端建立连接，通道开启！");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端断开连接，通道关闭！");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        //接收的消息
        SocketData res = JSON.parseObject(msg.text(), SocketData.class);
        Integer type = res.getRequestType();
        switch (type) {
            case 0:   // 建立连接
                websocketLogin(res, ctx);
                break;
            case 2: // 志愿者初始化成功
                toBlindJoin(res);
                break;
        }
        ctx.channel().writeAndFlush(new TextWebSocketFrame("收到客户端的数据"));
//        System.out.println(String.format("收到客户端%s的数据：%s", ctx.channel().id(), msg.text()));
    }

    /**
     * 志愿者初始化成功,向盲人转发 - 2
     * @param socketData
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
    }


    /**
     * 匹配成功,向志愿者发送信息 - 1
     *
     * @param socketData
     */
    public void matchSuccess(SocketData socketData) {

        if (cmap.containsKey(socketData.getVolunteerPhone())) {
            Channel channel = cmap.get(socketData.getVolunteerPhone());

            String response = this.getResponse(0, "匹配成功", 1, socketData);
            channel.writeAndFlush(new TextWebSocketFrame(response));
        }else{
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该用户未连接服务器");
        }
    }


    /**
     * 初始化socket - 0
     * 返回type,0
     *
     * @param socketData
     * @param ctx
     */
    private void websocketLogin(SocketData socketData, ChannelHandlerContext ctx) {
        String phone = socketData.getVolunteerPhone();
        if (StrUtil.isBlankIfStr(phone)) {
            phone = socketData.getBlindPhone();
        }
        cmap.put(phone, ctx.channel());
        System.out.println(phone + "登录");

        String response = this.getResponse(0, "初始化成功", 0, socketData);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
    }


    private String getResponse(Integer code, String message, Integer requestType, SocketData socketData) {
        SocketVO socketVO = new SocketVO();
        socketVO.setSocketData(socketData);
        socketVO.setCode(code);
        socketVO.setMessage(message);
        socketData.setRequestType(requestType);
        socketVO.setSocketData(socketData);
        return JSONUtil.toJsonStr(socketVO);
    }


    /**
     * 向家属紧急求助 - 3
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

    }
}