package com.niu.netty.protobuf.client;

import com.niu.netty.protobuf.server.UserMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private volatile int COUNT = 1;
    @Autowired
    private NettyClient nettyClient;

    /**
     * 建立连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("建立链接时：" + new Date());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("关闭连接时：" + new Date());
        final EventLoop eventExecutors = ctx.channel().eventLoop();
        nettyClient.doConnect(new Bootstrap(), eventExecutors);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("循环请求的时间：" + new Date() + ", 次数" + COUNT);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (IdleState.WRITER_IDLE.equals(event.state())) {
                UserMsg.UserOrBuilder userOrBuilder = UserMsg.User.newBuilder().setState(2);
                ctx.channel().writeAndFlush(userOrBuilder);
                COUNT++;
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof UserMsg.User)) {
            System.out.println("未知数据" + msg);
            return;
        }
        try {
            UserMsg.User user = (UserMsg.User) msg;
            System.out.println("客户端接收到的用户信息，编号：" + user.getId() + ", 姓名:" + user.getName()
            + ", 年龄" + user.getAge());
            UserMsg.UserOrBuilder userOrBuilder = UserMsg.User.newBuilder().setState(1);
            ctx.writeAndFlush(userOrBuilder);
            System.out.println("成功发送给服务端");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
