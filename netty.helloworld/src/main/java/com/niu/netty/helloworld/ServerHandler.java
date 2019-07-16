package com.niu.netty.helloworld;

import io.netty.channel.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Date;

@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        String response;
        boolean close = false;
        if (s.isEmpty()) {
            response = "Please type someting!.\r\n";
        } else if ("bye".equals(s.toLowerCase())) {
            response = "Have a good time";
            close = true;
        } else  {
            response = "Did you say " + s + "?\r\n";
        }
        ChannelFuture future = channelHandlerContext.write(response);
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write("Welcome to" + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.write("It is " + new Date() + " now.\r\n");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
