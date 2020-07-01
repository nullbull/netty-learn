package com.niu.netty.rpc.thrift;

import com.niu.netty.rpc.server.INiuServer;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import com.niu.netty.rpc.zookeeper.ZooKeeperServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;

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
            throw new IllegalAccessException("the tProcessor cant be null");
        }
        try {
            tServerTransport = new TNonblockingServerSocket(serverPublisher.port);
            NiuThreadedSelectorServer
        }
    }

    @Override
    public void stop() {

    }
}
