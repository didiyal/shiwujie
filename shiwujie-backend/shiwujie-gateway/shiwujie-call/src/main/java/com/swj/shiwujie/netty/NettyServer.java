package com.swj.shiwujie.netty;


import com.swj.shiwujie.constants.NettyConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
@Slf4j
public class NettyServer {

    @Resource
    private MyWebsocketChannelHandler myWebsocketChannelHandler;


    //初始化成功后就启动
    @PostConstruct
    public void init() {
        // 使用独立线程启动 Netty，避免阻塞 Spring 主线程
        new Thread(this::start).start();
    }

    public void start(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(myWebsocketChannelHandler);
            log.info("服务端开启等待客户端连接...");

            Channel channel = bootstrap.bind(NettyConstants.PORT).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("服务端启动失败", e);
        } finally {
            // 退出程序
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            log.info("服务端已关闭");
        }
    }
}
