package com.niu.netty.rpc.netty;

import com.niu.netty.rpc.server.INiuServer;
import com.niu.netty.rpc.server.NiuDefaultThreadFactory;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import com.niu.netty.rpc.utils.NiuThreadSelectorWorkerExecutorUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 17:31
 * @desc:
 */
@Slf4j
public class NettyServer implements INiuServer {

    private AbstractNiuServerPublisher serverPublisher;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private ExecutorService executorService;

    public NettyServer(AbstractNiuServerPublisher serverPublisher) {
        this.serverPublisher = serverPublisher;
    }

    @Override
    public void run() {
        try {
            if (Epoll.isAvailable()) {
                bossGroup = new EpollEventLoopGroup(serverPublisher.bossThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_EVENT_LOOP_THREADS : serverPublisher.bossThreadCount);
                workGroup = new EpollEventLoopGroup(serverPublisher.workThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_EVENT_LOOP_THREADS * 2 : serverPublisher.workThreadCount);
            } else {
                bossGroup = new NioEventLoopGroup(serverPublisher.bossThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_EVENT_LOOP_THREADS : serverPublisher.bossThreadCount);
                workGroup = new NioEventLoopGroup(serverPublisher.workThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_EVENT_LOOP_THREADS * 2 : serverPublisher.workThreadCount);
            }
            executorService = NiuThreadSelectorWorkerExecutorUtil.getWorkExecutorWithQueue(serverPublisher.niuThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_NIU_THREADS : serverPublisher.niuThreadCount,
                    serverPublisher.niuThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_NIU_THREADS :serverPublisher.niuThreadCount, serverPublisher.workQueue, new NiuDefaultThreadFactory(serverPublisher.serviceInterface.getName()));
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup).channel(workGroup instanceof EpollEventLoopGroup ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new NettySI)
        }
    }

    @Override
    public void stop() { }
}
