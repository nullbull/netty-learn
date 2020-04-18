package com.niu.netty.rpc.netty.handler;

import com.niu.netty.rpc.protol.NiuBinaryProtocol;
import com.niu.netty.rpc.protol.NiuTrace;
import com.niu.netty.rpc.server.domain.ErrorType;
import com.niu.netty.rpc.transport.TNiuFramedTransport;
import com.niu.netty.rpc.utils.IPUtil;
import com.niu.netty.rpc.utils.NiuExceptionUtil;
import com.niu.netty.rpc.utils.NiuRsaUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
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
            if (b[8] != (byte)1 || !(b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second)) {
                log.error("rsa error the client is not ras support!  className={}", className);

            }
        }

    }
    public static void handlerException(byte[] b, ChannelHandlerContext ctx, Exception e, ErrorType type, String privateKey, String publicKey, boolean thriftNative) {
        String serverIp = IPUtil.getIPV4();
        String exceptionMessage = NiuExceptionUtil.getExceptionInfo(e);
        String value = MessageFormat.format("error from server: {0} invoke error : {1}", serverIp, exceptionMessage);

        boolean isUserProtocol;

        //TODO
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
