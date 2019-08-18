package com.niu.netty.rpc.zookeeper;

import com.niu.netty.rpc.server.config.ZooKeeperConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

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
                zooKeeper = new ZooKeeper()
            }
        }
    }
}
