package com.swj.shiwujie.netty;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.JsonObject;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.config.NettyConfig;
import com.swj.shiwujie.constants.NettyConstants;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.WebsocketVO;
import com.swj.shiwujie.model.domain.Family;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.UserService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: Netty-WebSocket
 * @description: 接收处理并响应客户端WebSocket请求的核心业务处理类
 * @author: 01
 * @create: 2018-11-03 17:34
 **/
@Slf4j
@ChannelHandler.Sharable
@Component
public class MyWebsocketHandler extends SimpleChannelInboundHandler<Object> {

    private static final AttributeKey<Long> USER_ID_ATTR = AttributeKey.newInstance("userId");
    private WebSocketServerHandshaker handshaker;

    private final static Map<Long,Channel> channelPool = new HashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private FamilyService familyService;


    /**
     * 服务端处理客户端WebSocket请求的核心方法
     *
     * @param ctx ctx
     * @param msg msg
     * @throws Exception Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 处理客户端向服务端发起http握手请求的业务
        if (msg instanceof FullHttpRequest) {
            handHttpRequest(ctx, (FullHttpRequest) msg);
        }
        // 处理websocket连接
        else if (msg instanceof WebSocketFrame) {
            handWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String s = handshakeComplete.requestUri();
            HttpHeaders entries = handshakeComplete.requestHeaders();
            /**
             * 实现自己的初始化操作
             */
//            log.debug("s = {}",s);
//            log.debug("entries = {}",entries);
            String substring = s.substring(20);
            Long userId = Long.valueOf(substring);
            // 将 userId 存储到 Channel 的属性中
            ctx.channel().attr(USER_ID_ATTR).set(userId);
            channelPool.put(userId,ctx.channel());
            log.debug("----------------------");
            log.debug("用户创建socket连接"+userId+"目前总数量"+channelPool.size());

        }

        super.userEventTriggered(ctx, evt);
    }

    /**
     * 处理客户端与服务端之间的websocket业务
     *
     * @param ctx   ctx
     * @param frame frame
     */
    private void handWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否是关闭websocket的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
            log.debug("接收到关闭websocket的指令");
        }

        // 判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            log.debug("接收到ping消息");
            return;
        }

        // 判断是否是二进制消息，如果是二进制消息，则抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            log.error("目前不支持二进制消息");
            throw new UnsupportedOperationException("【" + this.getClass().getName() + "】不支持的消息");
        }

        // 获取客户端向服务端发送的消息
        String requestStr = ((TextWebSocketFrame) frame).text();
        Long userId = ctx.channel().attr(USER_ID_ATTR).get();
        log.debug("服务端收到客户端{}的消息: {}", userId,requestStr);
        JSONObject jsonObject = JSONUtil.parseObj(requestStr);
        String title = (String) jsonObject.get("title");
        if("CALL_HELP".equals(title)){
            this.handlerCALLHELP(jsonObject,ctx);
        }


//        // 群发，服务端向每个连接上来的客户端群发消息
//        channelPool.forEach((remoteId,channel) -> {
//            if(remoteId != this.userId){
//                // 返回应答消息
//                String responseStr = "用户" + this.userId +
//                        " ===>>> " + remoteId + requestStr;
//                TextWebSocketFrame tws = new TextWebSocketFrame(responseStr);
//                channel.writeAndFlush(tws);
//                log.debug("发消息完成. 发的消息为: {}", responseStr);
//            }
//        });

    }


    /**
     * 处理紧急求助服务
     * @param jsonObject
     * @param ctx
     */
    private void handlerCALLHELP(JSONObject jsonObject,ChannelHandlerContext ctx){
        Long userId = ctx.channel().attr(USER_ID_ATTR).get();
        String blindUid = (String) jsonObject.get("blindUid");
        //家庭的校验
        User user = userService.getById(userId);
        Long familyId = user.getFamilyId();
        if (familyId == null || familyId == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "需要加入家庭才可以紧急求助");
        }
        if (!familyService.familyUsersVerify(familyId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您的家庭没有可以求助的用户");
        }
        //寻找家庭的其他用户
        Family family = familyService.getById(familyId);
        List<Long> familyUserIdsList = familyService.getFamilyUserIdsList(family);
//        familyUserIdsList.forEach((userId1)->{
//            if(userId1 != this.userId){
//                //对非自己的其他人求助
//                Channel channel = channelPool.get(userId1);
//                //创建应答消息
//                String responseStr = new WebsocketVO("CALL_HELP",blindUid).toString();
//                channel.writeAndFlush(responseStr);
//            }
//        });
        for(Long id:  familyUserIdsList){
            if(!userId.equals(id)){
                //对非自己的其他人求助
                Channel channel = channelPool.get(id);
                if(channel != null){//给在线的家属发
                    //创建应答消息
                    log.debug("给用户{}发送消息",id);
                    JSONObject responseStrJSON = new JSONObject(new WebsocketVO("CALL_HELP", blindUid));
                    String responseStr = JSONUtil.toJsonStr(responseStrJSON);
                    TextWebSocketFrame tws = new TextWebSocketFrame(responseStr);
                    channel.writeAndFlush(tws);
                }
            }
        }
    }



    /**
     * 处理客户端向服务端发起http握手请求的业务
     *
     * @param ctx     ctx
     * @param request request
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String upgrade = request.headers().get(NettyConstants.UPGRADE_STR);
        // 非websocket的http握手请求处理
        if (!request.decoderResult().isSuccess() || !NettyConstants.WEBSOCKET_STR.equals(upgrade)) {
            sendHttpResponse(ctx, request,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            log.warn("非websocket的http握手请求");
            return;
        }

        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(NettyConstants.WEB_SOCKET_URL, null, false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null) {
            // 响应不支持的请求
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            log.warn("不支持的请求");
        } else {
            handshaker.handshake(ctx.channel(), request);
            log.debug("正常处理");
        }
    }

    /**
     * 服务端主动向客户端发送消息
     *
     * @param ctx      ctx
     * @param request  request
     * @param response response
     */
    private void sendHttpResponse(ChannelHandlerContext ctx,
                                  FullHttpRequest request,
                                  DefaultFullHttpResponse response) {
        // 不成功的响应
        if (response.status().code() != NettyConstants.OK_CODE) {
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
            log.warn("不成功的响应");
        }

        // 服务端向客户端发送数据
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(request) ||
                response.status().code() != NettyConstants.OK_CODE) {
            // 如果是非Keep-Alive，或不成功都关闭连接
            channelFuture.addListener(ChannelFutureListener.CLOSE);
            log.info("websocket连接关闭");
        }
    }

    /**
     * 客户端与服务端创建连接的时候调用
     *
     * @param ctx ctx
     * @throws Exception Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 将channel添加到channel group中
//        NettyConfig.GROUP.add(ctx.channel());
//        channelPool.put(,ctx.channel());
        log.info("客户端与服务端连接开启...");
    }

    /**
     * 客户端与服务端断开连接的时候调用
     *
     * @param ctx ctx
     * @throws Exception Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        Long userId = ctx.channel().attr(USER_ID_ATTR).get();
        channelPool.remove(userId);
        log.info("用户 {} 断开连接，剩余在线数: {}", userId, channelPool.size());
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     *
     * @param ctx ctx
     * @throws Exception Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 清空数据
        ctx.flush();

        log.info("flush数据 {}", ctx.name());
    }

    /**
     * 工程出现异常的时候调用
     *
     * @param ctx   ctx
     * @param cause cause
     * @throws Exception Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 打印异常堆栈
        log.error("WebSocket连接异常");
        Long userId = ctx.channel().attr(USER_ID_ATTR).get();
        channelPool.remove(userId);
        log.info("-----用户 {} 断开连接，剩余在线数: {}", userId, channelPool.size());
        // 主动关闭连接
        ctx.close();

    }
}