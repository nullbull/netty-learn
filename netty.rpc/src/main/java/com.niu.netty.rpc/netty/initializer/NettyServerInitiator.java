package com.niu.netty.rpc.netty.initializer;

import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.AllArgsConstructor;

import java.util.concurrent.ExecutorService;

/**
 * @author: niuzhenhao
 * @date: 2019-10-15 20:00
 * @desc:
 */
@AllArgsConstructor
public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private ExecutorService executorService;

    private AbstractNiuServerPublisher serverPublisher;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast("decoder", new NiuDecoder());
        socketChannel.pipeline().addLast("encoder", new NiuEncoder());
        socketChannel.pipeline().addLast("com.niu.netty.rpc/netty/handler", new NiuHandler(serverPublisher, executorService))
    }
}
