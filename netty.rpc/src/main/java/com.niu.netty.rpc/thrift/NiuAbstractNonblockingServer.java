package com.niu.netty.rpc.thrift;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TByteArrayOutputStream;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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
                           TProcessor tGenericProcessor,
                           boolean cat) {
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
