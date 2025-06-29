package com.swj.shiwujie;

import com.swj.shiwujie.constants.NettyConstants;
import com.swj.shiwujie.netty.MyWebsocketChannelHandler;
import com.swj.shiwujie.netty.NettyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@Slf4j
public class ShiwujieApplication  {

    public static void main(String[] args)  {
        SpringApplication.run(ShiwujieApplication.class, args);


    }



}
