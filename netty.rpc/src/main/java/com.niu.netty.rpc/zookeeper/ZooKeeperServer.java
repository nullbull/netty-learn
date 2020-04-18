package com.niu.netty.rpc.zookeeper;

import com.alibaba.fastjson.JSONObject;
import com.niu.netty.rpc.server.config.ZooKeeperConfig;
import com.niu.netty.rpc.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 17:36
 * @desc:
 */
@Slf4j
public class ZooKeeperServer {
    private static final int RETRY_TIMES = 2;
    private static final int SESSION_TIMEOUT = 3000;
    private static final String UTF_8 = "UTF-8";
    private ZooKeeperConfig zkConfig;
    private ZooKeeper zooKeeper = null;
    public ZooKeeperServer(ZooKeeperConfig zkConfig) {
        if (null == zkConfig) { throw new IllegalArgumentException("zookServerConfig can't be null"); }
        this.zkConfig = zkConfig;
    }
    public void init() {
        String env = zkConfig.getEnv();
        String service = zkConfig.getService();
        int port = zkConfig.getPort();
        int weight = zkConfig.getWeight();
        String zkPath = zkConfig.getZkPath();
        String server = zkConfig.getServer();

        CountDownLatch c = new CountDownLatch(1);
        if (null == zooKeeper) {
            try {
                zooKeeper = new ZooKeeper(zkPath, SESSION_TIMEOUT, new NiuWatcher(c));
            } catch (IOException e) {
                log.error("zk server faild service:" + env + "-" + service, e);
            }
        }

        try {
            int retry = 0;
            boolean connected = false;
            while (retry++ < RETRY_TIMES) {
                if (c.await(5, TimeUnit.SECONDS)) {
                    connected = true;
                    break;
                }
            }
            if (!connected) {
                log.error("zk connected fail! :" + env + "-" + service);
                throw new IllegalArgumentException("zk connected fail!");
            }
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        if (null == zooKeeper) {
            log.error("zk server is null service:" + env + "-" + service);
            throw new IllegalArgumentException("zk server can't be null");
        }
        try {
            String envPath = env.startsWith("/") ? env : "/".concat(env);
            if (null == zooKeeper.exists(envPath, null)) {
                zooKeeper.create(envPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String servicePath = service.startsWith("/") ? service : "/".concat(service);
            if (null == zooKeeper.exists(envPath + servicePath, null)) {
                zooKeeper.create(envPath + servicePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            JSONObject jsonChildData = new JSONObject();
            jsonChildData.put("weight", weight == 0 ? 10 : weight);
            jsonChildData.put("enable", 1);
            jsonChildData.put("server", server);
            String ip = IPUtil.getIPV4();
            if (StringUtils.isEmpty(ip)) {
                throw  new IllegalArgumentException("ip can't be null");
            }
            String childPathData = jsonChildData.toJSONString();
            String childPath;
            if (zooKeeper.exists(childPath = (envPath + servicePath + "/" + ip + ":" + port), null) == null) {
                zooKeeper.create(childPath, childPathData.getBytes(UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
            log.info("zk server init success, info{}", zkConfig);
        } catch (KeeperException e) {
            log.error (e.getMessage (), e);
        } catch (InterruptedException e) {
            log.error (e.getMessage (), e);
        } catch (UnsupportedEncodingException e) {
            log.error (e.getMessage (), e);
        }
    }

    public void destory() {
        if (null != zooKeeper) {
            try {
                zooKeeper.close();
                zooKeeper = null;
            } catch (Exception e) {
                log.error ( "the service 【{}】zk close faild info={}", zkConfig );

            }
        }
    }
    private class NiuWatcher implements Watcher {

        private CountDownLatch countDownLatch;
        NiuWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void process(WatchedEvent event) {
            if (Event.KeeperState.SyncConnected == event.getState()) {
                log.info("the service {} is SynConnected! ", IPUtil.getIPV4());
                countDownLatch.countDown();
            }
            if (Event.KeeperState.Expired == event.getState()) {
                log.warn("the service {} is expired! ", IPUtil.getIPV4());
                reConnected();
            }
            if (Event.KeeperState.Disconnected == event.getState()) {
                log.warn("the service {} is Disconnected! ", IPUtil.getIPV4());
            }
        }
        private void reConnected() {
            ZooKeeperServer.this.destory();
        }
    }
}
