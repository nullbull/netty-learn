package com.niu.netty.rpc.thrift;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author niuzhenhao
 * @date 2020/7/1 16:31
 * @desc
 */
@Slf4j
public class NiuThreadSelectorServer extends NiuAbstractNonblockingServer {

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

        public Args executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Args acceptQueueSizePerThread(int acceptQueueSizePerThread) {
            this.acceptQueueSizePerThread = acceptQueueSizePerThread;
            return this;
        }


        public Args selectorThreads(int i) {
            selectorThreads = i;
            return this;
        }

        public Args workerThreads(int i ) {
            workerThreads = i;
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

        public int getSelectorThreads() {
            return selectorThreads;
        }

        public int getWorkerThreads() {
            return workerThreads;
        }

        public int getStopTimeoutVal() {
            return stopTimeoutVal;
        }

        public TimeUnit getStopTimeoutUnit() {
            return stopTimeoutUnit;
        }

        public ExecutorService getExecutorService() {
            return executorService;
        }

        public int getAcceptQueueSizePerThread() {
            return acceptQueueSizePerThread;
        }

        public AcceptPolicy getAcceptPolicy() {
            return acceptPolicy;
        }

    }
    private volatile boolean stopped = true;

    private AcceptThread  acceptThread;

    private final Set<SelectorThread> selectorThreads = new HashSet<>();

    private final ExecutorService invoker;

    private final Args args;

    private String privateKey;

    private String publicKey;

    private String serviceName;

    private TProcessor tGenericProcessor;

    public NiuThreadSelectorServer(Args args) {
        super(args);
        args.validate();
        invoker = args.executorService == null ? createDefaultExecutor(args) : args.executorService;
        this.args = args;
    }

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

    @Override
    protected boolean startThreads() {
        try {
            for (int i = 0; i < args.selectorThreads; ++i) {
                selectorThreads.add(new SelectorThread(args.acceptQueueSizePerThread));
            }
            acceptThread = new AcceptThread((TNonblockingServerTransport) serverTransport_, new SelectorThreadLoadBalancer(selectorThreads));
            stopped = false;
            for (SelectorThread thread : selectorThreads) {
                thread.start();
            }
            acceptThread.start();
            return true;
        } catch (IOException e) {
            log.error("Failed to start threads!", e);
            return false;
        }
    }

    @Override
    protected void waitForShutdown() {
        try {
            joinThreads();
        } catch (InterruptedException e) {
            log.error("Interrupted while joining threads!", e);
        }
        gracefullyShutdownInvokerPool();
    }

    protected void joinThreads() throws InterruptedException {
        acceptThread.join();
        for (SelectorThread thread : selectorThreads) {
            thread.join();
        }
    }


    @Override
    public void stop() {
        stopped = true;
        stopListenting();

        if (acceptThread != null) {
            acceptThread.wakeupSelector();
        }
        if (selectorThreads != null) {
            for (SelectorThread thread : selectorThreads) {
                if (thread != null) {
                    thread.wakeUpSelector();
                }
            }
        }
    }

    @Override
    protected boolean requestInvoke(FrameBuffer frameBuffer) {
        return false;
    }

    @Override
    public void serve() {

    }
    protected void gracefullyShutdownInvokerPool() {
        invoker.shutdown();
        long timeoutMS = args.stopTimeoutUnit.toMillis(args.stopTimeoutVal);
        long now = System.currentTimeMillis();
        while (timeoutMS >= 0) {
            try {
                invoker.awaitTermination(timeoutMS, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException ix) {
                long newNow = System.currentTimeMillis();
                timeoutMS -= (newNow - now);
                now = newNow;
            }
        }
    }

    protected class AcceptThread extends Thread {

        private final TNonblockingServerTransport serverTransport;

        private final Selector acceptSelector;

        private final SelectorThreadLoadBalancer threadChooser;

        public AcceptThread(TNonblockingServerTransport serverTransport, SelectorThreadLoadBalancer threadChooser) throws IOException {
            this.serverTransport = serverTransport;
            this.threadChooser = threadChooser;
            this.acceptSelector = SelectorProvider.provider().openSelector();
            this.serverTransport.registerSelector(acceptSelector);
        }

        @Override
        public void run() {
            try {
                while (!stopped) {
                    select();
                }
            } catch (Throwable t) {
                log.error("run() exiting due to uncaught error", t);
            } finally {
                NiuThreadSelectorServer.this.stop();
            }
        }
        public void wakeupSelector() {
            acceptSelector.wakeup();
        }

        public void select() {
            try {
                acceptSelector.select();

                Iterator<SelectionKey> selectedKeys = acceptSelector.selectedKeys().iterator();
                while (!stopped && selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        handleAccept();
                    } else {
                        log.warn("Unexpected state in select! " + key.interestOps());
                    }
                }
            } catch (IOException e) {
                log.warn("Got an IOException while selecting!", e);
            }
        }

        private void handleAccept() {
            final TNonblockingTransport client = doAccept();
            if (client != null) {
                final SelectorThread targetThread = threadChooser.nextThread();
                if (args.acceptPolicy == Args.AcceptPolicy.FAST_ACCEPT || invoker == null) {
                    doAddAccept(targetThread, client);
                } else {
                    try {
                        invoker.submit(() -> {doAddAccept(targetThread, client);});
                    } catch (RejectedExecutionException rx) {
                        log.warn("ExecutorService rejected accept registration!", rx);
                        client.close();
                    }
                }
            }
        }
        private TNonblockingTransport doAccept() {
            try {
                return (TNonblockingTransport) serverTransport.accept();
            } catch (TTransportException e) {
                log.warn("Exception trying to accept!", e);
                return null;
            }
        }

        private void doAddAccept(SelectorThread thread, TNonblockingTransport client) {
            if (!thread.addAcceptedConnection(client)) {
                client.close();
            }
        }
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

    protected static ExecutorService createDefaultExecutor(Args options) {
        return (options.workerThreads > 0) ? Executors.newFixedThreadPool(options.workerThreads) : null;
    }
}
