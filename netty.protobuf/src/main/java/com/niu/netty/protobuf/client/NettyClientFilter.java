package com.niu.netty.protobuf.client;

import com.niu.netty.protobuf.server.UserMsg;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

public class NettyClientFilter extends ChannelInitializer<SocketChannel> {

    @Autowired
    private NettyClientHandler nettyClientHandler;
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline ph = socketChannel.pipeline();
        ph.addLast(new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));
        ph.addLast(new ProtobufVarint32FrameDecoder());
        ph.addLast(new ProtobufDecoder(UserMsg.User.getDefaultInstance()));
        ph.addLast(new ProtobufVarint32LengthFieldPrepender());
        ph.addLast(new ProtobufEncoder());
        ph.addLast("nettyClientHandler", new NettyClientHandler());
    }
}
