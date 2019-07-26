package com.niu.netty.protobuf.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class NettyClient {
    public static int PORT = 1234;

    private static EventLoopGroup boss = new NioEventLoopGroup();
    private static NettyClient nettyClient = new NettyClient();
    private static Bootstrap bs = new Bootstrap();
    private static volatile boolean INIT_FLAG = true;
    public static void main(String[] args) {
        nettyClient.run();
    }
    public void run() {
        doConnect(new Bootstrap(), boss);
    }
    public void doConnect(Bootstrap bs, EventLoopGroup boss) {
        try {
            bs.group(boss)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new NettyClientFilter())
                    .remoteAddress("127.0.0.1", PORT);
            ChannelFuture cf = bs.connect().addListener((ChannelFuture c) -> {
                final EventLoop eventLoop = c.channel().eventLoop();
                if (!c.isSuccess()) {
                    System.out.println("与服务端断开链接！在10s之后准备重试");
                    eventLoop.schedule(() -> doConnect(new Bootstrap(), eventLoop), 10, TimeUnit.SECONDS);
                }
            });
            if (INIT_FLAG) {
                System.out.println("Netty客户端启动成功");
                INIT_FLAG = false;
            }
        } catch (Exception e) {
            System.out.println("客户端链接失败" + e.getMessage());
        }
    }

}
