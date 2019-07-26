package com.niu.netty.protobuf.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("nettyServer")
public class NettyServer {
    private static final int PORT = 1234;
    private static EventLoopGroup boss = new NioEventLoopGroup();
    private static EventLoopGroup work = new NioEventLoopGroup();
    private static ServerBootstrap sb = new ServerBootstrap();

    @Autowired
    private NettyServerFilter nettyServerFilter;

    public void run() {
        try {
            sb.group(boss, work)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(nettyServerFilter);
            ChannelFuture cf = sb.bind(PORT);
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

}
