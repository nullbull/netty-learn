package com.niu.netty.rpc.register;

import com.niu.netty.rpc.client.cluster.RemoteServer;
import com.niu.netty.rpc.client.cluster.impl.ZookeeperClusterImpl;
import com.niu.netty.rpc.utils.IPUtil;
import heartbeat.request.HeartBeat;
import heartbeat.service.HeartbeatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author: niuzhenhao
 * @date: 2019-08-27 15:17
 * @desc:
 */
@Slf4j
public class ZookeeperClient {

    private static final int RETRY_TIMES = 2;
    private static final int SESSION_TIMEOUT = 3000;
    public static final Watcher NULL = null;

    public static final String UTF_8 = "UTF-8";

    public static final int TIMEOUT = 3000;

    public static final byte HEARTBEAT = (byte) 2;

    private String env;

    private String path;

    private String serviceName;

    private ZooKeeper zooKeeper = null;

    private ZookeeperClusterImpl zookeeperCluster;

    private CountDownLatch firstInitChild = new CountDownLatch(1);

    private List<RemoteServer> serverList = new CopyOnWriteArrayList<>();

    private Map<String, HeartbeatService.Client> serverHeartbeatMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public List<RemoteServer> getServerList() {
        return serverList;
    }

    public ZookeeperClient(String env, String path, String serviceName, ZookeeperClusterImpl zookeeperCluster) {
        if (null == env) {
            throw new RuntimeException("env can't be null");
        }
        if (null == serviceName) {
            throw new RuntimeException("serviceName can't be null");
        }
        if (null == path) {
            throw new RuntimeException("zookeeper ip and port can't be null");
        }
        this.env = env;
        this.path = path;
        this.serviceName = serviceName;
        this.zookeeperCluster = zookeeperCluster;
    }
    public void initZookeeper() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        if (null == zooKeeper) {
            try {
                zooKeeper = new ZooKeeper(path, SESSION_TIMEOUT, new ClientInitWatcher(c));
            }
        }
    }
    public void destroy() {
        serverList = null;
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
            executorService = null;
        }
        serverHeartbeatMap = null;
        if (null != zooKeeper) {
            try {
                zooKeeper.close();
                zooKeeper = null;
            } catch (Exception e) {
                log.error("the service [{}] zk close faild", env.concat(serviceName));
            }
        }
    }
    private class HeartbeatRUn implements Runnable {

        @Override
        public void run() {
            try {
                zookeeperCluster.writeLock.unlock();
                if (null != serverHeartbeatMap && !serverHeartbeatMap.isEmpty()) {
                    Iterator it = serverHeartbeatMap.entrySet().iterator();
                    in:
                    while (it.hasNext()) {
                        String str = ((Map.Entry<String, HeartbeatService.Client>)it.next()).getKey();
                        String ip = str.split("-")[0];
                        String port = str.split("-")[1];
                    }
                }
            }
        }
    }

    private class ClientInitWatcher implements Watcher {

        private CountDownLatch countDownLatch;

        ClientInitWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }
        @Override
        public void process(WatchedEvent watchedEvent) {
            if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
                log.info("the service{}-{}-{} is SyncConnected!", IPUtil.getIPV4(), ZookeeperClient.this.env,  ZookeeperClient.this.serviceName);
                countDownLatch.countDown();
            }
            if (Event.KeeperState.Expired == watchedEvent.getState()) {
                log.info("the service{}-{}-{} is expired!", IPUtil.getIPV4(), ZookeeperClient.this.env,  ZookeeperClient.this.serviceName);
                reConnected();
            }
            if (Event.KeeperState.Disconnected == watchedEvent.getState()) {
                log.warn("the service{}-{}-{} is disconnected!", IPUtil.getIPV4(), ZookeeperClient.this.env,  ZookeeperClient.this.serviceName);
            }
        }

        private void reConnected() {
            try {
                zookeeperCluster.
            }
        }
    }
}
