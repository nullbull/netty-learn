package com.niu.netty.rpc.netty.handler;

import com.niu.netty.rpc.exceptions.RSAException;
import com.niu.netty.rpc.heartbeat.impl.HeartbeatServiceImpl;
import com.niu.netty.rpc.heartbeat.service.HeartbeatService;
import com.niu.netty.rpc.protol.NiuBinaryProtocol;
import com.niu.netty.rpc.protol.NiuTrace;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import com.niu.netty.rpc.server.domain.ErrorType;
import com.niu.netty.rpc.transport.TNiuFramedTransport;
import com.niu.netty.rpc.utils.IPUtil;
import com.niu.netty.rpc.utils.NiuExceptionUtil;
import com.niu.netty.rpc.utils.NiuRsaUtil;
import com.niu.netty.rpc.utils.TraceThreadContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TIOStreamTransport;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

@Slf4j
@AllArgsConstructor
public class NiuHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private TProcessor tProcessor;

    private TProcessor genericTprocessor;

    private ExecutorService executorService;

    private String privateKey;

    private String publicKey;

    private String className;

    private int maxLength;

    public NiuHandler(AbstractNiuServerPublisher serverPublisher, ExecutorService executorService) {
        this.executorService = executorService;
        this.privateKey = serverPublisher.getPrivateKey();
        this.publicKey = serverPublisher.getPublicKey();
        this.className = serverPublisher.getServiceInterface().getName();
        this.tProcessor = serverPublisher.getTProcessor();
        this.maxLength = serverPublisher.getMaxLength();
        this.genericTprocessor = serverPublisher.getGenericTProcessor();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int i = msg.readableBytes();
        byte[] b = new byte[i];
        msg.readBytes(b);

        TMessage tMessage;
        NiuTrace niuTrace;
        boolean isUserProtocol;
        boolean generic;
        String genericMethodName;
        boolean thriftNative;
        if (b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second) {
            isUserProtocol = true;
            NiuMessage niuMessage = getNiuMessage(b);
            tMessage = niuMessage.getTMessage();
            niuTrace = niuMessage.getNiuTrace();
            generic = niuMessage.generic;
            genericMethodName = niuMessage.genericMethodName;
            thriftNative = niuMessage.isThriftNative();
        } else {
            isUserProtocol = false;
            NiuMessage niuMessage = getNiuMessage(b);
            tMessage = niuMessage.getTMessage();
            niuTrace = niuMessage.getNiuTrace();
            generic = niuMessage.generic;
            genericMethodName = niuMessage.genericMethodName;
            thriftNative = niuMessage.isThriftNative();
        }
        TProcessor localTProcessor;
        if (!generic) {
            localTProcessor = tProcessor;
        } else {
            localTProcessor = genericTprocessor;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport(inputStream);
        TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport(outputStream);

        TNiuFramedTransport inTransport = new TNiuFramedTransport(tioStreamTransportInput, 2048000);
        inTransport.setReadMaxLength_(maxLength);
        TNiuFramedTransport outTranport = new TNiuFramedTransport(tioStreamTransportOutput, 2048000);

        if (this.privateKey != null && this.publicKey != null) {
            if (b[8] != (byte) 1 || !(b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second)) {
                log.error("rsa error the client is not ras support!  className={}", className);
                handlerException(b, ctx, new RSAException("rsa Error"), ErrorType.APPLICATION, privateKey, publicKey, thriftNative);
                return;
            }
        }

            if (b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second) {

                if (b[7] == (byte)2) {
                    TProcessor tProcessorHeartBeat = new HeartbeatService.Processor<>(new HeartbeatServiceImpl());
                    outTranport.setHeartbeat((byte) 2);
                    TProtocolFactory tProtocolFactory = new NiuBinaryProtocol.Factory();
                    ((NiuBinaryProtocol.Factory) tProtocolFactory).setThriftNative(thriftNative);
                    TProtocol in = tProtocolFactory.getProtocol(inTransport);
                    TProtocol out = tProtocolFactory.getProtocol(outTranport);
                    try {
                        tProcessorHeartBeat.process(in, out);
                        ctx.writeAndFlush(outputStream);
                        return;
                    } catch (Exception e) {
                        log.error("heartbeat error e, className:{}", className);
                        handlerException(b, ctx, e, ErrorType.APPLICATION, privateKey, publicKey, thriftNative);
                        return;
                    }
                }
                    if (b[8] == (byte)1) {
                        inTransport.setPrivateKey(privateKey);
                        inTransport.setPublicKey(this.publicKey);

                        outTranport.setRsa((byte) 1);
                        outTranport.setPrivateKey(this.privateKey);
                        outTranport.setPublicKey(this.publicKey);
                    }
                }
            TProtocolFactory tProtocolFactory = new NiuBinaryProtocol.Factory();
            ((NiuBinaryProtocol.Factory) tProtocolFactory).setThriftNative(thriftNative);
            TProtocol in = tProtocolFactory.getProtocol(inTransport);
            TProtocol out = tProtocolFactory.getProtocol(outTranport);

            String methodName = generic ? genericMethodName : tMessage.name;
            try {
                executorService.execute(new NettyRunnable(ctx, in, out, outputStream, localTProcessor, b, privateKey, publicKey, className, methodName, niuTrace, thriftNative));
            } catch (RejectedExecutionException e) {
                log.error(e.getMessage() + ErrorType.THREAD + ", className:" + className, e);
                handlerException(b, ctx, e, ErrorType.THREAD, privateKey, publicKey, thriftNative);
            }
        }
    public static void handlerException(byte[] b, ChannelHandlerContext ctx, Exception e, ErrorType type, String privateKey, String publicKey, boolean thriftNative) {
        String serverIp = IPUtil.getIPV4();
        String exceptionMessage = NiuExceptionUtil.getExceptionInfo(e);
        String value = MessageFormat.format("error from server: {0} invoke error : {1}", serverIp, exceptionMessage);

        boolean isUserProtocol;
        if (b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second) {
            isUserProtocol = true;
        } else {
            isUserProtocol = false;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport(inputStream);
        TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport(outputStream);

        TNiuFramedTransport inTransport = new TNiuFramedTransport(tioStreamTransportInput);

        TNiuFramedTransport outTransport = new TNiuFramedTransport(tioStreamTransportOutput, 16384000, isUserProtocol);

        TProtocolFactory tProtocolFactory = new NiuBinaryProtocol.Factory();

        ((NiuBinaryProtocol.Factory)tProtocolFactory).setThriftNative(thriftNative);

        TProtocol in = tProtocolFactory.getProtocol(inTransport);
        TProtocol out = tProtocolFactory.getProtocol(outTransport);

        if (isUserProtocol) {
            if (b[8] == (byte)1) {
                if (!(e instanceof RSAException)) {
                    inTransport.setPrivateKey(privateKey);
                    inTransport.setPublicKey(publicKey);

                    //out
                    outTransport.setRsa((byte) 1);
                    outTransport.setPrivateKey(privateKey);
                    outTransport.setPublicKey(publicKey);
                } else {
                    try {
                        TMessage tMessage = new TMessage("", TMessageType.EXCEPTION, -1);
                        TApplicationException exception = new TApplicationException(9999, value);
                        out.writeMessageBegin(tMessage);
                        exception.write(out);
                        out.writeMessageEnd();
                        out.getTransport().flush();
                        ctx.writeAndFlush(outputStream);
                        return;
                    } catch (Exception e1) {
                        log.error(e1.getMessage(), e1);
                    }
                }
            } else {
                if (e instanceof RSAException) {
                    try {
                        TMessage tMessage = new TMessage("", TMessageType.EXCEPTION, -1);
                        TApplicationException exception = new TApplicationException(6699, value);
                        out.writeMessageBegin(tMessage);
                        exception.write(out);
                        out.writeMessageEnd();
                        out.getTransport().flush();
                        ctx.writeAndFlush(outputStream);
                        return;
                    } catch (Exception e2) {
                        log.error(e2.getMessage(), e2);
                    }
                }
            }
        }
        try {
            TMessage message = in.readMessageBegin();
            TProtocolUtil.skip(in, TType.STRUCT);
            in.readMessageEnd();
            TApplicationException tApplicationException = null;
            switch (type) {
                case THREAD:
                    tApplicationException = new TApplicationException(6666, value);
                    break;
                case APPLICATION:
                    tApplicationException = new TApplicationException(TApplicationException.INTERNAL_ERROR, value);
                    break;
            }
            out.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
            tApplicationException.write(out);
            out.writeMessageEnd();
            out.getTransport().flush();
            ctx.writeAndFlush(outputStream);
            log.info("handlerException:" + tApplicationException.getType() + ":" + value);
        } catch (TException e1) {
            log.error("unknown Exception:{} : {} {}", type, value, e1 );
            ctx.close();
        }
    }
    @AllArgsConstructor
    public static class NettyRunnable implements Runnable {
        private ChannelHandlerContext ctx;
        private TProtocol in;
        private TProtocol out;
        private ByteArrayOutputStream outputStream;
        private TProcessor tprocessor;
        private byte[] b;
        private String privateKey;
        private String publicKey;
        private String className;
        private String methodName;
        private NiuTrace niuTrace;
        private boolean thriftNative;

        @Override
        public void run() {
            if (StringUtils.isNotEmpty(methodName)) {
                if (niuTrace.getRootId() != null) {
                    String rootId = niuTrace.getRootId();
                    String childId = niuTrace.getChildId();
                    String parentId = niuTrace.getParentId();
                    NiuTrace currentNiuTrace = new NiuTrace();
                    currentNiuTrace.setParentId(childId);
                    currentNiuTrace.setRootId(rootId);
                    TraceThreadContext.set(currentNiuTrace);
                }
            }
            try {
                tprocessor.process(in, out);
                ctx.writeAndFlush(outputStream);
            } catch (Exception e) {
                log.error(e.getMessage() + ErrorType.APPLICATION + ", className:" + className, e);
                handlerException(this.b, ctx, e, ErrorType.APPLICATION, privateKey, publicKey, thriftNative);
            } finally {

            }
        }
    }

    private NiuMessage getTMessage(byte[] b){
        byte[] buff = new byte[b.length-4];
        System.arraycopy (  b,4,buff,0,buff.length);
        ByteArrayInputStream inputStream = new ByteArrayInputStream ( buff );
        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
        TProtocol tBinaryProtocol = new NiuBinaryProtocol ( tioStreamTransportInput,true );
        TMessage tMessage=null;
        NiuTrace koalasTrace;
        String genericMethodName;
        boolean generic;
        boolean thriftNative;
        try {
            tMessage= tBinaryProtocol.readMessageBegin ();
            koalasTrace = ((NiuBinaryProtocol) tBinaryProtocol).getNiuTrace ();
            generic = ((NiuBinaryProtocol) tBinaryProtocol).isGeneric ();
            genericMethodName = ((NiuBinaryProtocol) tBinaryProtocol).getGenericMethodName ();
            thriftNative = ((NiuBinaryProtocol) tBinaryProtocol).isThriftNative ();
        } catch (Exception e) {
            return new NiuMessage(new TMessage(), new NiuTrace (),false,StringUtils.EMPTY,false);
        }
        return new NiuMessage(tMessage,koalasTrace,generic,genericMethodName,thriftNative);
    }

    private NiuMessage getNiuMessage(byte[] b) {
        byte[] buff = new byte[b.length - 4];
        System.arraycopy(b, 4, buff, 0, buff.length);
        int size = buff.length;
        byte[] request = new byte[size - 6];
        byte[] header = new byte[6];
        System.arraycopy(buff, 6, request, 0, size - 6);
        System.arraycopy(buff, 0, header, 0, 6);

        if (header[4] == (byte) 1) {
            byte[] signLenByte = new byte[4];
            System.arraycopy(buff, 6, signLenByte, 0, 4);
            int signLen = TNiuFramedTransport.decodeFrameSize(signLenByte);
            byte[] signByte = new byte[signLen];
            System.arraycopy(buff, 10, signByte, 0, signLen);
            String sign = "";

            try {
                sign = new String(signByte, "UTF-8");
            } catch (Exception e) {
                return niuMessageSupplier.get();
            }

            byte[] rsaBody = new byte[size - 10 -signLen];
            System.arraycopy(buff, 10 + signLen, rsaBody, 0, size - 10 -signLen);
            try {
                if (!NiuRsaUtil.verify(rsaBody, publicKey, sign)) {
                    return niuMessageSupplier.get();
                }
                request = NiuRsaUtil.decryptByPrivateKey(rsaBody, privateKey);
            }catch (Exception e) {
                return niuMessageSupplier.get();
            }
        }
        TMessage tMessage;
        NiuTrace niuTrace;
        boolean generic;
        String genericMethodName;
        boolean thriftNative;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(request);
        TIOStreamTransport tioStreamTransport = new TIOStreamTransport(inputStream);
        try {
            TProtocol tBinaryProtocol = new NiuBinaryProtocol(tioStreamTransport, true);
            tMessage = tBinaryProtocol.readMessageBegin();
            niuTrace = ((NiuBinaryProtocol) tBinaryProtocol).getNiuTrace();
            generic = ((NiuBinaryProtocol) tBinaryProtocol).isGeneric();
            genericMethodName = ((NiuBinaryProtocol) tBinaryProtocol).getGenericMethodName();
            thriftNative = ((NiuBinaryProtocol) tBinaryProtocol).isThriftNative();
        } catch (Exception e) {
            return niuMessageSupplier.get();
        }
        return new NiuMessage(tMessage, niuTrace, generic, genericMethodName, thriftNative);
    }

    private Supplier<NiuMessage> niuMessageSupplier = () -> new NiuMessage(new TMessage(), new NiuTrace(), false, StringUtils.EMPTY, false);

    @Data
    @AllArgsConstructor
    private static class NiuMessage {
        private TMessage tMessage;
        private NiuTrace niuTrace;
        private boolean generic;
        private String genericMethodName;
        private boolean thriftNative;
    }
}
