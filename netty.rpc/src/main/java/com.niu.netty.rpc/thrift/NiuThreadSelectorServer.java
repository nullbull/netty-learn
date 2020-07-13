package com.niu.netty.rpc.thrift;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerTransport;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author niuzhenhao
 * @date 2020/7/1 16:31
 * @desc
 */
@Slf4j
public class NiuThreadSelectorServer extends NiuAbstractNonblockingServer {

    @Data
    public static class Args extends AbstractNonblockingServerArgs<Args> {

        private int selectorThreads = 2;

        private int workerThreads = 5;
        private int stopTimeoutVal = 60;
        private TimeUnit stopTimeoutUnit = TimeUnit.SECONDS;
        private ExecutorService executorService = null;

        private int acceptQueueSizePerThread = 4;

        public static enum AcceptPolicy {
            FAIR_ACCEPT,

            FAST_ACCEPT;
        }

        private AcceptPolicy acceptPolicy = AcceptPolicy.FAST_ACCEPT;

        public Args(TNonblockingServerTransport transport) {
            super(transport);
        }

        public Args(TServerTransport transport) {
            super(transport);
        }

        public Args acceptPolicy(AcceptPolicy acceptPolicy) {
            this.acceptPolicy = acceptPolicy;
            return this;
        }

        public void validate() {
            if (selectorThreads <= 0) {
                throw new IllegalArgumentException("selectorThreads must be positive.");
            }
            if (workerThreads < 0) {
                throw new IllegalArgumentException("workerThreads must be non-negative.");
            }
            if (acceptQueueSizePerThread <= 0) {
                throw new IllegalArgumentException("acceptQueueSizePerThread must be positive.");
            }

        }

    }
    private volatile boolean stopped = true;

    private AcceptThread  acceptThread;

    private final Set<SelectorThread> selectorThreads = new HashMap<>();

    private final ExecutorService invoker;

    private final Args args;

    private String privateKey;

    private String publicKey;

    private String serviceName;

    private TProcessor tGenericProcessor;


    //todo 2020-07-13
    @Override
    protected boolean startThreads() {
        return false;
    }

    @Override
    protected void waitForShutdown() {

    }

    @Override
    protected boolean requestInvoke(FrameBuffer frameBuffer) {
        return false;
    }

    @Override
    public void serve() {

    }
}
