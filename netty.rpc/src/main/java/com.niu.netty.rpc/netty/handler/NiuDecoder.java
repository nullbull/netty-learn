package com.niu.netty.rpc.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author: niuzhenhao
 * @date: 2019-10-15 20:07
 * @desc:
 */
@Slf4j
public class NiuDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        try {
            if (byteBuf.readableBytes() < 4) {
                return;
            }
            byteBuf.markReaderIndex();
            byte[] b = new byte[4];
            byteBuf.readBytes(b);
            int length = decodeFrameSize(b);
            if (byteBuf.readableBytes() < length) {
                byteBuf.resetReaderIndex();
                return;
            }
            byteBuf.resetReaderIndex();
            ByteBuf fream = byteBuf.readRetainedSlice(4 + length);
            byteBuf.resetReaderIndex();
            byteBuf.skipBytes(4 + length);
            list.add(fream);
        } catch (Exception e) {
            log.error("decode error", e);
        }
    }
    //将byte[4] 转为 32int
    private static int decodeFrameSize(byte[] buf) {
        return (buf[0] & 255) << 24 | (buf[1] << 16) | (buf[2] & 255 << 8) | buf[3] & 255;
    }
}
