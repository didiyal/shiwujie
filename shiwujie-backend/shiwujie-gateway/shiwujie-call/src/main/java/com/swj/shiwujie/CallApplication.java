package com.swj.shiwujie;

import com.swj.shiwujie.socket.CoordinationNettyServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@EnableDubbo
@SpringBootApplication
public class CallApplication implements CommandLineRunner {

    public static void main(String[] args)  {
        SpringApplication.run(CallApplication.class, args);


    }

    @Autowired
    private CoordinationNettyServer nettyServer;

    @Override
    public void run(String... args) throws Exception {
        nettyServer.start();
    }

}
