package com.niu.netty.protobuf.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.springframework.stereotype.Service;

@Service
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private int IDLE_COUNT = 1;
    private int count = 1;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("链接的客户端地址:" + ctx.channel().remoteAddress());
        UserMsg.User user = UserMsg.User.newBuilder()
                .setId(1)
                .setAge(22)
                .setName("niuzhenhao")
                .setState(0)
                .build();
        ctx.writeAndFlush(user);
        super.channelActive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (IdleState.READER_IDLE.equals(event.state())) {
                System.out.println("已经5秒没有收到客户端的信息了");
                if (IDLE_COUNT > 1) {
                    System.out.println("关闭这个不活跃的channel");
                    ctx.channel().close();
                }
            }
            IDLE_COUNT++;
        }
     }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("第" + count +  "，服务端接收的消息：" + msg);
        try {
            if (msg instanceof UserMsg.User) {
                UserMsg.User user = (UserMsg.User) msg;
                if (user.getState() == 1) {
                    System.out.println("客户端业务处理成功");
                } else if (user.getState() == 2) {
                    System.out.println("接收到客户端发送的心跳");
                } else {
                    System.out.println("未知命令");
                    return;
                }
            } else {
                System.out.println("位置命令");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
