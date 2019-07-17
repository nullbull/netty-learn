package com.niu.netty.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    private HttpHeaders headers;

    private HttpRequest request;

    private FullHttpRequest fullHttpRequest;

    private static final String FAVICON_ICO = "/favicon.ico";
    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject msg) throws Exception {
        User user = new User();
        user.setUserName("Justinniu");
        user.setDate(new Date());

        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            headers = request.headers();
            String uri = request.uri();
            log.info("http uri: " + uri);
            if (uri.equals(FAVICON_ICO)) {
                return;
            }
            HttpMethod method =  request.method();
            if (method.equals(HttpMethod.GET)) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri, Charset.forName("UTF-8"));
                Map<String, List<String>> uriAttributes = queryStringDecoder.parameters();
                uriAttributes.entrySet().stream().forEach(k -> {
                    System.out.println(k.getKey() + "=" + k.getValue()); });
            user.setMethod("get");
            } else if (method.equals(HttpMethod.POST)) {
                fullHttpRequest = (FullHttpRequest) msg;
                dealWithContentType();
                user.setMethod("post");
            }
            JSONSerializer jsonSerializer = new JSONSerializer();
            byte[] content = jsonSerializer.serialize(user);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content));
            response.headers().set(CONTENT_TYPE, "text/plain");
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (!keepAlive) {
                channelHandlerContext.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, KEEP_ALIVE);
                channelHandlerContext.write(response);
            }
        }
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

    private void dealWithContentType() throws Exception {
        String contentType = getContentType();

        if (contentType.equals("application/json")) {
            String jsonStr = fullHttpRequest.content().toString(Charset.forName("UTF-8"));
            JSONObject object = JSON.parseObject(jsonStr);
            object.entrySet().stream().forEach(k -> {
                log.info(k.getKey() + "=" + k.getValue());
            });
        } else if (contentType.equals("application/x-www-form-urlencoded")) {
            String jsonStr = fullHttpRequest.content().toString(Charset.forName("UTF-8"));
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(jsonStr, false);
            Map<String, List<String>> uriAttributes = queryStringDecoder.parameters();
            uriAttributes.entrySet().stream().forEach(k -> { log.info(k.getKey() + "=" + k.getValue()); });
        }
    }

    private String getContentType() {
        String typeStr = headers.get("Content-Type").toString();
        String[] list = typeStr.split(";");
        return list[0];
    }
}
