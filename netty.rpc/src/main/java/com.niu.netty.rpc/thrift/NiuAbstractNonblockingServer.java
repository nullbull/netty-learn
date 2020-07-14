package com.niu.netty.rpc.thrift;

import com.niu.netty.rpc.exceptions.RSAException;
import com.niu.netty.rpc.heartbeat.impl.HeartbeatServiceImpl;
import com.niu.netty.rpc.heartbeat.service.HeartbeatService;
import com.niu.netty.rpc.protol.NiuBinaryProtocol;
import com.niu.netty.rpc.server.domain.ErrorType;
import com.niu.netty.rpc.transport.TNiuFramedTransport;
import com.niu.netty.rpc.utils.IPUtil;
import com.niu.netty.rpc.utils.NiuExceptionUtil;
import com.niu.netty.rpc.utils.NiuRsaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.*;
import org.apache.zookeeper.Transaction;
import org.omg.PortableInterceptor.SUCCESSFUL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.thrift.transport.TFramedTransport.encodeFrameSize;

/**
 * @author niuzhenhao
 * @date 2020/7/1 16:32
 * @desc
 */

@Slf4j
public abstract class NiuAbstractNonblockingServer extends TServer {

    public static abstract class AbstractNonblockingServerArgs<T extends AbstractNonblockingServerArgs<T>> extends AbstractServerArgs<T> {
        public long maxReadBufferBytes = Long.MAX_VALUE;
        public AbstractNonblockingServerArgs(TServerTransport transport) {
            super(transport);
            transportFactory(new TFastFramedTransport.Factory());
        }
    }
    private final long MAX_READ_BUFFER_BYTES;

    private final AtomicLong readBufferBytesAllocated = new AtomicLong(0);

    public NiuAbstractNonblockingServer(AbstractNonblockingServerArgs args) {
        super(args);
        MAX_READ_BUFFER_BYTES = args.maxReadBufferBytes;
    }

    public void server() {
        if (!startThreads()) {
            return;
        }
        if (!startListening()) {
            return;
        }
        setServing(true);
        waitForShutdown();
        setServing(false);
        stopListenting();
    }

    protected abstract boolean startThreads();

    protected abstract void waitForShutdown();

    protected boolean startListening() {
        try {
            serverTransport_.listen();
            return true;
        } catch (TTransportException ttxx) {
            log.error("Failed to start listening on server socket!", ttxx);
            return false;
        }
    }

    protected void stopListenting() {
        serverTransport_.close();
    }

    protected abstract boolean requestInvoke(FrameBuffer frameBuffer);

    protected abstract class AbstractSelectThread extends Thread {
        protected final Selector selector;

        protected final Set<FrameBuffer> selectInterestChanges = new HashSet<>();
        //todo

        public AbstractSelectThread() throws IOException {
            this.selector = SelectorProvider.provider().openSelector();
        }
        public void wakeUpSelector() {
            selector.wakeup();
        }
        public void requestSelectInterestChange(FrameBuffer frameBuffer) {
            synchronized (selectInterestChanges) {
                selectInterestChanges.add(frameBuffer);
            }
            selector.wakeup();
        }

        protected void processInterestChanges() {
            synchronized (selectInterestChanges) {
                for (FrameBuffer fb : selectInterestChanges) {
                    fb.changeSelectInterests();
                }
                selectInterestChanges.clear();
            }
        }

        protected void handleRead(SelectionKey key) {
            FrameBuffer buffer = (FrameBuffer) key.attachment();
            if (!buffer.read()) {
                cleanUpSelectionKey(key);
                return;
            }
            if (buffer.isFrameFullyRead()) {
                if (!requestInvoke(buffer)) {
                    cleanUpSelectionKey(key);
                }
            }
        }

        protected void handleWrite(SelectionKey key) {
            FrameBuffer buffer = (FrameBuffer) key.attachment();
            if (!buffer.write()) {
                cleanUpSelectionKey(key);
            }
        }

        protected void cleanUpSelectionKey(SelectionKey key) {
            FrameBuffer buffer = (FrameBuffer) key.attachment();
            if (buffer != null) {
                buffer.close();
            }
            key.cancel();
        }
     }

    private enum FrameBufferState {
        READING_FRAME_SIZE,
        READING_FRAME,
        READ_FRAME_COMPLETE,
        AWAITING_REGISTER_WRITE,
        WRITING,
        AWAITING_REGISTER_READ,
        AWAITING_CLOSE
    }

    protected class FrameBuffer {
        private final TNonblockingTransport trans_;

        private final SelectionKey selectionKey_;

        private final AbstractSelectThread selectThread_;

        private FrameBufferState state_ = FrameBufferState.READING_FRAME_SIZE;

        private ByteBuffer buffer_;

        private TByteArrayOutputStream response_;

        private String privateKey;
        private String publicKey;
        private String serviceName;
        private String methodName;
        private boolean generic;
        private boolean thriftNative;
        private TProcessor tGenericProcessor;

        public FrameBuffer(final TNonblockingTransport trans,
                           final SelectionKey selectionKey,
                           final AbstractSelectThread selectThread,
                           String privateKey,
                           String publicKey,
                           String serviceName,
                           TProcessor tGenericProcessor) {
            this(trans, selectionKey, selectThread);
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.serviceName = serviceName;
            this.tGenericProcessor = tGenericProcessor;
        }

        public FrameBuffer(final TNonblockingTransport trans, final SelectionKey selectionKey, final AbstractSelectThread selectThread) {
            trans_ = trans;
            selectionKey_ = selectionKey;
            selectThread_ = selectThread;
            buffer_ = ByteBuffer.allocate(4);
        }

        public boolean read() {
            if (state_ == FrameBufferState.READING_FRAME_SIZE) {
                if (!internalRead()) {
                    return false;
                }
                if (buffer_.remaining() == 0) {
                    int frameSize = buffer_.getInt(0);
                    if (frameSize <= 0) {
                        log.error("Read an invalid frame size of " + frameSize + ". Are you using TFramedTransport on the client side?");
                        return false;
                    }
                    if (frameSize > MAX_READ_BUFFER_BYTES) {
                        log.error("Read a frame size of " + frameSize
                                + ", which is bigger than the maximum allowable buffer size for ALL connections.");
                        return false;
                    }
                    if (readBufferBytesAllocated.get() + frameSize > MAX_READ_BUFFER_BYTES) {
                        return true;
                    }
                    readBufferBytesAllocated.addAndGet(frameSize);

                    buffer_ = ByteBuffer.allocate(frameSize);

                    state_ = FrameBufferState.READING_FRAME;
                } else {
                    return true;
                }
            }
            if (state_ == FrameBufferState.READING_FRAME) {
                if (!internalRead()) {
                    return false;
                }
                if (buffer_.remaining() == 0) {
                    selectionKey_.interestOps(0);
                    state_ = FrameBufferState.READ_FRAME_COMPLETE;
                }
                return true;
            }
            log.error("Read was called but state is invalid (" + state_ + ")");
            return false;
        }

        public boolean write() {
            if (state_ == FrameBufferState.WRITING) {
                try {
                    if (trans_.write(buffer_) < 0) {
                        return false;
                    }
                } catch (Exception e) {
                    log.warn("Got an IOException during write!", e);
                    return false;
                }
                if (buffer_.remaining() == 0) {
                    prepareRead();
                }
                return true;
            }
            log.error("Write was called, but state is invalid (" + state_ + ")");
            return false;
        }

        public void changeSelectInterests() {
            if (state_ == FrameBufferState.AWAITING_REGISTER_WRITE) {
                selectionKey_.interestOps(SelectionKey.OP_WRITE);
                state_ = FrameBufferState.WRITING;
            } else if (state_ == FrameBufferState.AWAITING_REGISTER_READ) {
                prepareRead();
            } else if (state_ == FrameBufferState.AWAITING_CLOSE) {
                close();
                selectionKey_.cancel();
            } else {
                log.error("changeSelectInterest was called, but state is invalid (" + state_ + ")");
            }
        }

        private boolean internalRead() {
            try {
                if (trans_.read(buffer_) < 0) {
                    return false;
                }
                return true;
            } catch (IOException e) {
                log.warn("Got an IOException in internalRead!", e);
                return false;
            }
        }

        public void close() {
            if (state_ == FrameBufferState.READING_FRAME || state_ == FrameBufferState.READ_FRAME_COMPLETE) {
                readBufferBytesAllocated.addAndGet(-buffer_.array().length);
            }
            trans_.close();
        }

        public boolean isFrameFullyRead() {
            return state_ == FrameBufferState.READ_FRAME_COMPLETE;
        }

        public void responseReady() {
            readBufferBytesAllocated.addAndGet(-buffer_.array().length);

            if (response_.len() == 0) {
                state_ = FrameBufferState.AWAITING_REGISTER_READ;
                buffer_ = null;
            } else {
                buffer_ = ByteBuffer.wrap(response_.get(), 0 , response_.len());
                state_ = FrameBufferState.AWAITING_REGISTER_WRITE;
            }
            requestSelectInterestChange();
        }

        private void prepareRead() {
            selectionKey_.interestOps(SelectionKey.OP_READ);
            buffer_ = ByteBuffer.allocate(4);
            state_ = FrameBufferState.READING_FRAME_SIZE;
        }

        private void requestSelectInterestChange() {
            if (Thread.currentThread() == this.selectThread_) {
                changeSelectInterests();
            } else {
                this.selectThread_.requestSelectInterestChange(this);
            }
        }

        public void invoke() {
            try {
                TTransport inTrans = getInputTransport();
                TTransport outTrans = getOutputTransport();
                TProtocol inProt = inputProtocolFactory_.getProtocol(inTrans);
                TProtocol outProt = outputProtocolFactory_.getProtocol(outTrans);
                ((NiuBinaryProtocol) inProt).setThriftNative(thriftNative);
                ((NiuBinaryProtocol) outProt).setThriftNative(thriftNative);
                byte[] body = buffer_.array();
                //处理心跳检测
                if (body[0] == TNiuFramedTransport.first && body[1] == TNiuFramedTransport.second && body[3] == ((byte)2)) {
                    TProcessor tProcessorHeartBeat = new HeartbeatService.Processor<>(new HeartbeatServiceImpl());
                    ((TNiuFramedTransport) outTrans).setHeartbeat((byte)2);
                    tProcessorHeartBeat.process(inProt, outProt);
                } else {
                    try {
                        try {
                            if (!generic) {
                                processorFactory_.getProcessor(inTrans).process(inProt, outProt);
                            } else {
                                tGenericProcessor.process(inProt, outProt);
                            }
                        } catch (Exception e) {
                            byte[] _body = buffer_.array();
                            byte[] len = new byte[4];
                            encodeFrameSize(_body.length, len);
                            byte[] _b = new byte[_body.length + 4];
                            System.arraycopy(len, 0, _b, 0, 4);
                            System.arraycopy(_body, 0, _b, 4, _b.length);
                            handlerException(_b, e, ErrorType.APPLICATION, privateKey, publicKey);
                        }
                    } catch (Exception e) {
                        throw e;
                    }
                }
                responseReady();
                return;
            } catch (TException e) {
                log.warn("Exception while invoking!", e);
            } catch (Throwable t) {
                log.error("Unexpected throwable while invoking!", t);
            }
            state_ = FrameBufferState.AWAITING_CLOSE;
            requestSelectInterestChange();
        }

        public void handlerException(byte[] b, Exception e, ErrorType type, String privateKey, String publicKey) throws TException {

            String serverIp = IPUtil.getIPV4();
            String exceptionMessage = NiuExceptionUtil.getExceptionInfo(e);
            String value = MessageFormat.format("error from server: {0}  invoke error : {1}", serverIp, exceptionMessage);
            boolean ifUserProtocol;
            if (b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second) {
                ifUserProtocol = true;
            } else {
                ifUserProtocol = false;
            }
            ByteArrayInputStream inputStream  = new ByteArrayInputStream(b);
            response_ = new TByteArrayOutputStream();

            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport(inputStream);
            TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport(response_);

            TNiuFramedTransport inTransport = new TNiuFramedTransport(tioStreamTransportInput);
            TNiuFramedTransport outTransport = new TNiuFramedTransport(tioStreamTransportOutput);

            NiuBinaryProtocol.Factory tProcessorFactory = new NiuBinaryProtocol.Factory();
            tProcessorFactory.setThriftNative(thriftNative);
            TProtocol in = tProcessorFactory.getProtocol(inTransport);
            TProtocol out = tProcessorFactory.getProtocol(outTransport);


            TMessage tMessage= new TMessage("", TMessageType.EXCEPTION, -1);
            if (ifUserProtocol) {
                if (b[8] == (byte) 1) {
                    if (!(e instanceof RSAException)) {
                        inTransport.setPrivateKey(privateKey);
                        inTransport.setPublicKey(publicKey);

                        outTransport.setRsa((byte) 1);
                        outTransport.setPrivateKey(privateKey);
                        outTransport.setPublicKey(publicKey);
                    } else {
                        try {
                            TApplicationException exception = new TApplicationException(6699, value);
                            out.writeMessageBegin(tMessage);
                            exception.write(out);
                            out.writeMessageEnd();
                            out.getTransport().flush();
                            return;
                        } catch (Exception e2) {
                            log.error(e2.getMessage(), e2);
                            throw e2;
                        }
                    }
                } else {
                    if (e instanceof RSAException) {
                        try {
                            TApplicationException exception = new TApplicationException(6699, value);
                            out.writeMessageBegin(tMessage);
                            exception.write(out);
                            out.writeMessageEnd();
                            out.getTransport().flush();
                            return;
                        } catch (Exception e2) {
                            log.error(e2.getMessage(), e2);
                            throw e2;
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
                log.info("handlerException:" + tApplicationException.getType() + ":" + value);
            } catch (Exception e1) {
                log.error("unknown Exception:" + type + ":" + value, e1);
                throw e1;
            }

        }

        private TTransport getInputTransport() {
            byte[] body = buffer_.array();

            byte[] len = new byte[4];
            encodeFrameSize(body.length, len);
            byte[] b = new byte[body.length + 4];

            System.arraycopy(len, 0, b, 0, 4);
            System.arraycopy(body, 0, b, 4, body.length);
            TMessage tMessage;
            String genericMethodName;
            if (b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second) {
                NiuMessage niuMessage = getNiuTMessage(b);
                tMessage = niuMessage.gettMessage();
                generic = niuMessage.isGeneric();
                thriftNative = niuMessage.isThriftNative();
                genericMethodName = niuMessage.getGenericMethodName();
            } else {
                NiuMessage niuMessage = getTMessage(b);
                tMessage = niuMessage.gettMessage();
                generic = niuMessage.isGeneric();
                thriftNative = niuMessage.isThriftNative();
                genericMethodName = niuMessage.getGenericMethodName();
            }
            methodName = generic ? genericMethodName : tMessage.name;
            boolean isRsa = (b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second);
            if (this.privateKey != null && this.publicKey != null) {
                if (b[8] != (byte) 1 || !isRsa) {
                    throw  new RSAException("thrift server rsa error");
                }
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport(inputStream);
            TNiuFramedTransport inTransport = new TNiuFramedTransport(tioStreamTransportInput);
            if (isRsa && b[8] == (byte)1) {
                inTransport.setPrivateKey(this.privateKey);
                inTransport.setPublicKey(this.publicKey);
            }
            return inTransport;
        }

        private NiuMessage getNiuTMessage(byte[] b) {
            byte[] buff = new byte[b.length - 4];
            System.arraycopy(b, 4, buff, 0, buff.length);

            int size = buff.length;
            byte[] request = new byte[size - 6];
            byte[] head = new byte[6];
            System.arraycopy(buff, 6, request, 0, size - 6);
            System.arraycopy(buff, 0, head, 0,  6);

            if (head[4] == (byte) 1) {
                byte[] signLenByte = new byte[4];
                System.arraycopy(buff, 6, signLenByte, 0, 4);
                int signLen = TNiuFramedTransport.decodeFrameSize(signLenByte);
                byte[] signByte = new byte[signLen];
                System.arraycopy(buff, 10, signByte, 0, signLen);
                String sign = "";
                try {
                    sign = new String(signByte, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return new NiuMessage(new TMessage(), false, StringUtils.EMPTY, false);
                }
                byte[] rsaBody = new byte[size - 10 - signLen];
                System.arraycopy(buff, 10 + signLen, rsaBody, 0, size - 10 - signLen);

                try {
                    if (!NiuRsaUtil.verify(rsaBody, publicKey, sign)) {
                        return new NiuMessage(new TMessage(), false, StringUtils.EMPTY, false);
                    }
                    request = NiuRsaUtil.decryptByPrivateKey(rsaBody, privateKey);
                } catch (Exception e) {
                    return new NiuMessage(new TMessage(), false, StringUtils.EMPTY, false);
                }
            }

            TMessage tMessage;
            boolean generic;
            String genericMethodName;
            boolean thriftNative;

            ByteArrayInputStream inputStream = new ByteArrayInputStream(request);
            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport(inputStream);
            try {
                NiuBinaryProtocol tBinaryProtocol = new NiuBinaryProtocol(tioStreamTransportInput, true);
                tMessage = tBinaryProtocol.readMessageBegin();
                generic = tBinaryProtocol.isGeneric();
                genericMethodName = tBinaryProtocol.getGenericMethodName();
                thriftNative = tBinaryProtocol.isThriftNative();
            } catch (Exception e) {
                return new NiuMessage(new TMessage(), false, StringUtils.EMPTY, false);
            }
            return new NiuMessage(tMessage, generic, genericMethodName, thriftNative);
        }

        private TTransport getOutputTransport() {
            byte[] body = buffer_.array();

            byte[] len = new byte[4];
            encodeFrameSize(body.length, len);
            byte[] b = new byte[body.length + 4];

            System.arraycopy(len, 0, b, 0, 4);
            System.arraycopy(body, 0, b, 0, body.length);

            boolean ifUserProtocol = b[4] == TNiuFramedTransport.first && b[5] == TNiuFramedTransport.second;

            response_ = new TByteArrayOutputStream();
            TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport(response_);
            TNiuFramedTransport outTransport = new TNiuFramedTransport(tioStreamTransportOutput, 2048000, ifUserProtocol);

            if (ifUserProtocol && b[8] == (byte) 1) {
                outTransport.setRsa((byte) 1);
                outTransport.setPrivateKey(this.privateKey);
                outTransport.setPublicKey(this.publicKey);
            }
            return outTransport;
        }

        private NiuMessage getTMessage(byte[] b) {
            byte[] buff = new byte[b.length - 4];
            System.arraycopy(b, 4, buff, 0, buff.length);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buff);
            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport(inputStream);
            NiuBinaryProtocol tBinaryProtocol = new NiuBinaryProtocol(tioStreamTransportInput, true);
            TMessage tMessage;
            String genericMethodName;
            boolean thriftNative;
            try {
                tMessage = tBinaryProtocol.readMessageBegin();
                generic = tBinaryProtocol.isGeneric();
                genericMethodName = tBinaryProtocol.getGenericMethodName();
                thriftNative = tBinaryProtocol.isThriftNative();
            } catch (Exception e) {
                return new NiuMessage(new TMessage(), false, StringUtils.EMPTY, false);
            }
            return new NiuMessage(tMessage, generic, genericMethodName, thriftNative);

        }



        private class NiuMessage {
            private TMessage tMessage;
            private boolean generic;
            private String genericMethodName;
            private boolean thriftNative;

            public NiuMessage(TMessage tMessage,  boolean generic, String genericMethodName, boolean thriftNative) {
                this.tMessage = tMessage;
                this.generic = generic;
                this.genericMethodName = genericMethodName;
                this.thriftNative = thriftNative;
            }

            public boolean isThriftNative() {
                return thriftNative;
            }

            public void setThriftNative(boolean thriftNative) {
                this.thriftNative = thriftNative;
            }

            public String getGenericMethodName() {
                return genericMethodName;
            }

            public void setGenericMethodName(String genericMethodName) {
                this.genericMethodName = genericMethodName;
            }

            public TMessage gettMessage() {
                return tMessage;
            }

            public void settMessage(TMessage tMessage) {
                this.tMessage = tMessage;
            }


            public boolean isGeneric() {
                return generic;
            }

            public void setGeneric(boolean generic) {
                this.generic = generic;
            }
        }





    }

}
