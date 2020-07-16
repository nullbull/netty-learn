package com.niu.netty.rpc.thrift;

import com.niu.netty.rpc.protol.NiuBinaryProtocol;
import com.niu.netty.rpc.server.INiuServer;
import com.niu.netty.rpc.server.NiuDefaultThreadFactory;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import com.niu.netty.rpc.transport.TNiuFramedTransport;
import com.niu.netty.rpc.utils.NiuThreadSelectorWorkerExecutorUtil;
import com.niu.netty.rpc.zookeeper.ZooKeeperServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.util.concurrent.ExecutorService;

/**
 * @author niuzhenhao
 * @date 2020/7/1 16:26
 * @desc
 */

@Slf4j
public class ThriftServer implements INiuServer {

    private AbstractNiuServerPublisher serverPublisher;

    private TProcessor tProcessor;
    private TNonblockingServerSocket tServerTransport;
    private TServer tServer;

    private ExecutorService executorService;

    private ZooKeeperServer zooKeeperServer;


    public ThriftServer(AbstractNiuServerPublisher serverPublisher) {
        this.serverPublisher = serverPublisher;
    }

    @Override
    public void run() {
        tProcessor = serverPublisher.getTProcessor();
        if (tProcessor == null) {
            log.error("the tProcessor cant be null serverInfo={}", serverPublisher);
            throw new IllegalArgumentException("the tProcessor cant be null");
        }
        try {
            tServerTransport = new TNonblockingServerSocket(serverPublisher.port);
            NiuThreadSelectorServer.Args args = new NiuThreadSelectorServer.Args(tServerTransport);
            TNiuFramedTransport.Factory transprotFactory = new TNiuFramedTransport.Factory();
            TProtocolFactory tProtocolFactory = new NiuBinaryProtocol.Factory();
            args.transportFactory(transprotFactory);
            args.protocolFactory(tProtocolFactory);
            args.maxReadBufferBytes = serverPublisher.maxLength;
            args.processor(tProcessor);
            args.selectorThreads(serverPublisher.bossThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_EVENT_LOOP_THREADS : serverPublisher.bossThreadCount);
            args.workerThreads(serverPublisher.bossThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_EVENT_LOOP_THREADS  * 2: serverPublisher.workThreadCount);

            executorService = NiuThreadSelectorWorkerExecutorUtil.getWorkerExecutor(serverPublisher.niuThreadCount == 0 ? AbstractNiuServerPublisher.DEFAULT_NIU_THREADS : serverPublisher.niuThreadCount, new NiuDefaultThreadFactory(serverPublisher.serviceInterface.getName()));
            args.executorService( executorService);
            args.acceptQueueSizePerThread(AbstractNiuServerPublisher.DEFAULT_THRIFT_ACCEPT_THREAD);

            tServer = new NiuThreadSelectorServer(args);
            ((NiuThreadSelectorServer) tServer).setPrivateKey(serverPublisher.privateKey);
            ((NiuThreadSelectorServer) tServer).setPublicKey(serverPublisher.publicKey);
            ((NiuThreadSelectorServer) tServer).setServiceName(serverPublisher.getServiceInterface().getName());
            ((NiuThreadSelectorServer) tServer).settGenericProcessor(serverPublisher.getGenericTProcessor());

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    log.info("wait for service over 3000ms");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (tServer != null && tServer.isServing()) {
                        tServer.stop();
                    }
                    if (executorService != null) {
                        executorService.shutdown();
                    }
                }
            });
            new Thread(new ThriftRunable(tServer)).start();
        } catch (TTransportException e) {
            log.error("thrift server init faid service:" + serverPublisher.serviceInterface.getName(), e);
            stop();
            throw new IllegalArgumentException("thrift server init faid service:" + serverPublisher.serviceInterface.getName());
        }
        log.info("thrift server init success server={}", serverPublisher);
    }

    @Override
    public void stop() {

        log.info("wait for service over 3000ms");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        if (tServer != null && tServer.isServing()) {
            tServer.stop();
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        log.info("thrift server stop success server={}", serverPublisher);
    }
    private class ThriftRunable implements Runnable {

        private TServer tServer;

        public ThriftRunable(TServer tServer) {
            this.tServer = tServer;
        }

        @Override
        public void run() {
            tServer.serve();
        }
    }
}
