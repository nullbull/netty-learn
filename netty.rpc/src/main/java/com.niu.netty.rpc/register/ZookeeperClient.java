package com.niu.netty.rpc.register;

import com.alibaba.fastjson.JSONObject;
import com.niu.netty.rpc.client.cluster.RemoteServer;
import com.niu.netty.rpc.client.cluster.impl.ZookeeperClusterImpl;
import com.niu.netty.rpc.protol.NiuBinaryProtocol;
import com.niu.netty.rpc.transport.TNiuFramedTransport;
import com.niu.netty.rpc.utils.IPUtil;
import com.niu.netty.rpc.heartbeat.request.HeartBeat;
import com.niu.netty.rpc.heartbeat.service.HeartbeatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
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
                zooKeeper = new ZooKeeper(path, SESSION_TIMEOUT, new ClientInitWatcher(countDownLatch));
            } catch (IOException e) {
                log.error("zk server faild service:" + env + "-" + serviceName, e);
            }
        }
        try {
            int retry = 3;
            boolean connected = false;
            while (retry++ < RETRY_TIMES) {
                if (countDownLatch.await(5, TimeUnit.SECONDS)) {
                    connected = true;
                    break;
                }
            }
            if(!connected) {
                log.error("zk Client connected fail! :" + env + "-" + serviceName);
                throw new IllegalArgumentException("zk client connected fail!");
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        try {
            String envPath = env.startsWith("/") ? env : "/".concat(env);
            if (zooKeeper.exists(envPath, null) == null) {
                zooKeeper.create(envPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String servicePath = serviceName.startsWith("/") ? serviceName : "/".concat(serviceName);
            if (zooKeeper.exists(envPath + servicePath, null) == null) {
                zooKeeper.create(envPath + servicePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String fullPath = envPath + servicePath;
            Watcher w = new NiuWatcher();
            getChildren(fullPath, w);
            watchChildDateChange(fullPath, w);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } catch (KeeperException e) {
            log.error(e.getMessage(), e);
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
    private class HeartbeatRun implements Runnable {

        @Override
        public void run() {
            try {
                zookeeperCluster.writeLock.lock();
                if (null != serverHeartbeatMap && !serverHeartbeatMap.isEmpty()) {
                    Iterator it = serverHeartbeatMap.entrySet().iterator();
                    in:
                    while (it.hasNext()) {
                        String str = ((Map.Entry<String, HeartbeatService.Client>)it.next()).getKey();
                        String ip = str.split("-")[0];
                        String port = str.split("-")[1];

                        if (null != serverList) {
                            for (int i = serverList.size() - 1; i >= 0; i--) {
                                RemoteServer remoteServer = serverList.get(i);
                                if (ip.equals(remoteServer.getIp()) && port.equals(remoteServer.getPort())) {
                                    if (!remoteServer.isEnable()) {
                                        continue in;
                                    }
                                }
                            }
                        }

                        HeartbeatService.Client client = serverHeartbeatMap.get(str);
                        HeartBeat heartBeat = new HeartBeat();
                        heartBeat.setIp(IPUtil.getIPV4());
                        heartBeat.setServiceName(ZookeeperClient.this.serviceName);
                        heartBeat.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:MM:ss")));
                        int retry = 3;
                        while (retry-- > 0) {
                            try {
                                HeartBeat respone = client.getHeartBeat(heartBeat);
                                log.info("HeartBeat info:ip:{},serviceName:{}", respone.getIp(), serviceName);
                                break;
                            } catch (Exception e) {
                                if (0 == retry) {
                                    log.warn("HeartBeat error:{}", heartBeat);
                                    if (null != serverList) {
                                        for (int i = serverList.size() - 1; i >= 0; i--) {
                                            RemoteServer remoteServer = serverList.get(i);
                                            if (remoteServer.getIp().equals(ip) && remoteServer.getPort().equals(port)) {
                                                try {
                                                    serverList.remove(i);
                                                    it.remove();
                                                    if (zookeeperCluster.serverPoolMap.contains(str)) {
                                                        GenericObjectPool<TTransport> transport = zookeeperCluster.serverPoolMap.get(str);
                                                        zookeeperCluster.serverPoolMap.remove(str);
                                                        transport.close();
                                                    }
                                                    continue in;
                                                } catch (Exception e1) {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                zookeeperCluster.writeLock.unlock();
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
                zookeeperCluster.writeLock.lock();
                ZookeeperClient.this.destroy();
                firstInitChild = new CountDownLatch(1);
                serverList = new CopyOnWriteArrayList<>();
                //心跳服务
                serverHeartbeatMap = new ConcurrentHashMap<>();
                executorService = Executors.newScheduledThreadPool(1);
                ZookeeperClient.this.initZookeeper();
            } finally {
                zookeeperCluster.writeLock.unlock();
            }
        }
    }

    private class NiuWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getState().equals(Event.KeeperState.SyncConnected)) {
                if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                    String parentPath = watchedEvent.getPath();
                    log.info("the service {} is changed!", serviceName);
                    try {
                        firstInitChild.await();
                        List<String> childPaths = ZookeeperClient.this.zooKeeper.getChildren(parentPath, this);
                        ZookeeperClient.this.updateServerList(childPaths, parentPath);
                        log.info("the serviceList: {} !", childPaths);
                        for (String childPath : childPaths) {
                            String fullPath = parentPath.concat("/").concat(childPath);
                            ZookeeperClient.this.zooKeeper.getData(fullPath, this, new Stat());
                        }
                    } catch (KeeperException e) {
                        log.error(e.getMessage(), e);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
                String fullPath = watchedEvent.getPath();
                log.info("the service 【{}】 data {} is changed ! full mess is 【{}】", serviceName, fullPath);
                try {
                    firstInitChild.await();
                    String data = new String(ZookeeperClient.this.zooKeeper.getData(fullPath, this, new Stat()));
                    JSONObject json = JSONObject.parseObject(data);
                    String childPath = fullPath.substring(fullPath.lastIndexOf("/") + 1);
                    //192.168.3.2:6666
                    String ip = childPath.split(":")[0];
                    String port = childPath.split(":")[1];
                    String weight = json.getString("weight");
                    String enable = json.getString("enable");
                    String server = json.getString("server");

                    RemoteServer remoteServer = new RemoteServer(ip, port, Integer.parseInt(weight), "1".equals(enable), server);
                    ZookeeperClient.this.updateServer(remoteServer);
                } catch (KeeperException e) {
                    e.printStackTrace ();
                } catch (InterruptedException e) {
                    e.printStackTrace ();
                }
            }
        }
    }
    private void updateServer(RemoteServer remoteServer) {
        try {
            zookeeperCluster.writeLock.lock();
            if (null != serverList) {
                for (int i = 0; i < serverList.size(); i++) {
                    RemoteServer tempServer = serverList.get(i);
                    if (tempServer.getIp().equals(remoteServer.getIp()) && tempServer.getPort().equals(remoteServer.getPort())) {
                        serverList.set(i, remoteServer);
                        tempServer = null;
                    }
                }
            }
        } finally {
            zookeeperCluster.writeLock.unlock();
        }
    }

    private void updateServerList(List<String> childPaths, String parentPath) {
        try {
            zookeeperCluster.writeLock.lock();
            if (!serverList.isEmpty()) {
                serverList.clear();
            }
            if (!serverHeartbeatMap.isEmpty()) {
                serverHeartbeatMap.clear();
            }
            if (null != zookeeperCluster.serverPoolMap && !zookeeperCluster.serverPoolMap.isEmpty()) {
                Iterator it = zookeeperCluster.serverPoolMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, GenericObjectPool> e = (Map.Entry) it.next();
                    if (null != e.getValue()) {
                        e.getValue().close();
                    }
                    it.remove();
                }
            }
            for (String childPath : childPaths) {
                String currentPath = parentPath.concat("/").concat(childPath);
                try {
                    byte[] bytes = zooKeeper.getData(currentPath, null, new Stat());
                    try {
                        String data = new String(bytes, "UTF-8");
                        JSONObject json = JSONObject.parseObject(data);

                        String ip = childPath.split(":")[0];

                        String port = childPath.split(":")[1];

                        String weight = json.getString("weight");

                        String enable = json.getString("enable");

                        String server = json.getString("server");

                        RemoteServer remoteServer = new RemoteServer(ip, port, Integer.parseInt(weight), "1".equals(enable), server);
                        serverList.add(remoteServer);

                        //HeartBeat
                        TSocket t = new TSocket(remoteServer.getIp(), Integer.parseInt(remoteServer.getPort()), TIMEOUT);
                        TTransport tTransport = new TNiuFramedTransport(t);
                        ((TNiuFramedTransport) tTransport).setHeartbeat(HEARTBEAT);
                        TProtocol protocol = new NiuBinaryProtocol(tTransport);
                        HeartbeatService.Client client = new HeartbeatService.Client(protocol);
                        tTransport.open();
                        serverHeartbeatMap.put(zookeeperCluster.createMapKey(remoteServer), client);
                    } catch (UnsupportedEncodingException e) {
                        log.error (e.getMessage() + " UTF-8 is not allow!", e);
                    } catch (TTransportException e) {
                        log.error (e.getMessage(), e);
                    }
                } catch (KeeperException e) {
                    log.error (e.getMessage()+ "currPath is not exists!", e);
                } catch (InterruptedException e) {
                    log.error (e.getMessage() + "the current thread is Interrupted", e);
                }
            }
        } finally {
            zookeeperCluster.writeLock.unlock();
        }
    }

    private void getChildren(String path, Watcher w) {
        try {
            List<String> childPaths = this.zooKeeper.getChildren(path, w);
            updateServerList(childPaths, path);
        } catch (KeeperException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void watchChildDateChange(String path, Watcher w) {
        try {
            List<String> childPath = zooKeeper.getChildren(path, null);
            for (String child : childPath) {
                String fullPath = path.concat("/").concat(child);
                this.zooKeeper.getData(fullPath, w, new Stat());
            }
        } catch (KeeperException | InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            firstInitChild.countDown();
        }
    }
    private void initScheduled() {
        LocalTime localTime = LocalTime.now();
        LocalTime after = LocalTime.of(localTime.getHour(), localTime.getMinute(), 0);
        int time = (after.getNano() - localTime.getNano()) / (1000 * 1000);
        if (time >= 10 * 1000) {
            executorService.scheduleWithFixedDelay(new HeartbeatRun(), time / 1000, 60, TimeUnit.SECONDS);
        } else {
            executorService.scheduleWithFixedDelay(new HeartbeatRun(), time / 1000 + 60, 60, TimeUnit.SECONDS);
        }

    }
}