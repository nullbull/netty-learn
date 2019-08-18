package com.niu.netty.rpc.netty;

import com.niu.netty.rpc.server.INiuServer;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import io.netty.channel.EventLoopGroup;
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
    @Override
    public void run() {

    }

    @Override
    public void stop() {

    }
}
