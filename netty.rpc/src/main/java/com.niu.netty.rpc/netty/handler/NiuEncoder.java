package com.niu.netty.rpc.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;

public class NiuEncoder extends MessageToByteEncoder<ByteArrayOutputStream> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteArrayOutputStream byteArrayOutputStream, ByteBuf byteBuf) throws Exception {
        byteBuf.writeBytes(byteArrayOutputStream.toByteArray());
    }
}
