package com.swj.shiwujie.netty;

import com.swj.shiwujie.constants.NettyConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @program: Netty-WebSocket
 * @description: 初始化连接时的各个组件
 * @author: 01
 * @create: 2018-11-03 21:53
 **/
@Component
public class MyWebsocketChannelHandler extends ChannelInitializer<SocketChannel> {

    @Resource  // 注入 Spring 管理的 Handler
    private MyWebsocketHandler myWebsocketHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(NettyConstants.HTTP_CODEC, new HttpServerCodec())
                .addLast(NettyConstants.AGGREGATOR, new HttpObjectAggregator(NettyConstants.MAX_CONTENT_LENGTH))
                .addLast(new WebSocketServerProtocolHandler(
                        NettyConstants.WEBSOCKET_PATH,
                        null,
                        true,
                        65536 * 10,
                        false,
                        true))
                .addLast(NettyConstants.HTTP_CHUNKED, new ChunkedWriteHandler())
                .addLast(NettyConstants.HANDLER,myWebsocketHandler);
    }


}