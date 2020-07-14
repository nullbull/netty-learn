package com.niu.netty.rpc.thrift;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TServerTransport;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * @author niuzhenhao
 * @date 2020/7/1 16:31
 * @desc
 */
@Slf4j
public class NiuThreadSelectorServer extends NiuAbstractNonblockingServer {

    public NiuThreadSelectorServer(AbstractNonblockingServerArgs args) {
        super(args);
    }

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

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public AcceptThread getAcceptThread() {
        return acceptThread;
    }

    public void setAcceptThread(AcceptThread acceptThread) {
        this.acceptThread = acceptThread;
    }

    public Set<SelectorThread> getSelectorThreads() {
        return selectorThreads;
    }

    public ExecutorService getInvoker() {
        return invoker;
    }

    public Args getArgs() {
        return args;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public TProcessor gettGenericProcessor() {
        return tGenericProcessor;
    }

    public void settGenericProcessor(TProcessor tGenericProcessor) {
        this.tGenericProcessor = tGenericProcessor;
    }

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

    protected class AcceptThread extends Thread {

        private final TNonblockingServerTransport serverTransport;

        private final Selector acceptSelector;

        private final SelectorThreadLoadBalancer threadChooser;

        public

    }

    protected class SelectorThread extends AbstractSelectThread {

        private final BlockingQueue<TNonblockingTransport> acceptedQueue;

        public SelectorThread() throws IOException {
            this(new LinkedBlockingQueue<TNonblockingTransport>());
        }
        public SelectorThread(int maxPendingAccepts) throws IOException {
            this(createDefaultAcceptQueue(maxPendingAccepts));
        }

        public SelectorThread(BlockingQueue<TNonblockingTransport> acceptedQueue) throws IOException {
            this.acceptedQueue = acceptedQueue;
        }

        public boolean addAcceptedConnection(TNonblockingTransport accepted) {
            try {
                acceptedQueue.put(accepted);
            } catch (InterruptedException e) {
                log.warn("Interrupted while adding accepted connection!", e);
                return false;
            }
            selector.wakeup();
            return true;
        }
        @Override
        public void run() {
            try {
                while (!stopped) {
                    select();
                    processAcceptedConnections();
                    processInterestChanges();
                }
                for (SelectionKey selectionKey : selector.keys()) {
                    cleanUpSelectionKey(selectionKey);
                }
            } catch (Throwable t) {
                log.error("run() exiting due to uncaught error", t);
            } finally {
                NiuThreadSelectorServer.this.stop();
            }
        }

        private void select() {
            try {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                while (!stopped && selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        cleanUpSelectionKey(key);
                        continue;
                    }

                    if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    } else {
                        log.warn("Unexpected state in select! " + key.interestOps());
                    }
                }
            } catch (IOException e) {
                log.warn("Got an IOException while selecting!", e);
            }
        }
        private void processAcceptedConnections() {
            while (!stopped) {
                TNonblockingTransport accepted = acceptedQueue.poll();
                if (accepted == null) {
                    break;
                }
                registerAccepted(accepted);
            }
        }
        private void registerAccepted(TNonblockingTransport accepted) {
            SelectionKey clientKey = null;
            try {
                clientKey = accepted.registerSelector(selector, SelectionKey.OP_READ);

                FrameBuffer frameBuffer = new FrameBuffer(accepted, clientKey, SelectorThread.this, privateKey, publicKey, serviceName, tGenericProcessor);
                clientKey.attach(frameBuffer);
            } catch (IOException e) {
                log.warn("Failed to register accepted connection to selector!", e);
                if (clientKey != null) {
                    cleanUpSelectionKey(clientKey);
                }
                accepted.close();
            }
        }
    }

    private static BlockingQueue<TNonblockingTransport> createDefaultAcceptQueue(int queueSize) {
        if (queueSize == 0) {
            return new LinkedBlockingQueue<>();
        }
        return new ArrayBlockingQueue<>(queueSize);
    }

    protected class SelectorThreadLoadBalancer {
        private final Collection<? extends SelectorThread> threads;
        private Iterator<? extends SelectorThread> nextThreadIterator;

        public <T extends SelectorThread> SelectorThreadLoadBalancer(Collection<T> threads) {
            if (threads.isEmpty()) {
                throw new IllegalArgumentException("At least one selector thread is required");
            }
            this.threads = threads;
            nextThreadIterator = this.threads.iterator();
        }

        public SelectorThread nextThread() {
            if (!nextThreadIterator.hasNext()) {
                nextThreadIterator = threads.iterator();
            }
            return nextThreadIterator.next();
        }


    }
}
