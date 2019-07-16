package com.niu.netty.helloworld;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class ClientInitializer extends ChannelInitializer<SocketChannel> {

    private final static StringDecoder DECODER = new StringDecoder();
    private final static StringEncoder ENCODER = new StringEncoder();
    private final static ClientHandler CLIENT_HANDLER = new ClientHandler();
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline ch = socketChannel.pipeline();
        ch.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        ch.addLast(DECODER);
        ch.addLast(ENCODER);

        ch.addLast(CLIENT_HANDLER);
    }
}
