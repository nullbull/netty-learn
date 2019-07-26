package com.niu.netty.protobuf.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class NettyServerApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(NettyServerApplication.class, args);
        NettyServer nettyServer = context.getBean(NettyServer.class);
        nettyServer.run();
    }
}
