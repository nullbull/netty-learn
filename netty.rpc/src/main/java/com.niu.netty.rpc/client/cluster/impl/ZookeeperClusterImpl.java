package com.niu.netty.rpc.client.cluster.impl;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.RemoteServer;
import com.niu.netty.rpc.client.cluster.ServerObject;
import com.niu.netty.rpc.register.ZookeeperClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author: niuzhenhao
 * @date: 2019-08-27 15:14
 * @desc:
 */
@Slf4j
public class ZookeeperClusterImpl extends AbstractBaseIcluster {

    private String hostAndPorts;
    private String env;

    private ILoadBalancer loadBalancer;

    private String serviceName;

    private ZookeeperClient zookeeperClient;

    public ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public Lock writeLock = lock.writeLock();
    public Lock readLock = lock.readLock();

    public ZookeeperClusterImpl(String hostAndPorts, ILoadBalancer loadBalancer, String serviceName, String env, boolean async, int connectionTimeout, int soTimeout, GenericObjectPoolConfig genericObjectPoolConfig, AbandonedConfig abandonedConfig) {
        super(loadBalancer, serviceName, async, connectionTimeout, soTimeout, genericObjectPoolConfig, abandonedConfig);
        this.hostAndPorts = hostAndPorts;
        this.loadBalancer = loadBalancer;
        this.serviceName = serviceName;
        this.env = env;
        initZKClinet(hostAndPorts, serviceName, env);
    }

    @Override
    public RemoteServer getUseRemote() {
        RemoteServer remoteServer;
        try {
            readLock.lock();
            remoteServer = loadBalancer.select(zookeeperClient.getServerList());
        } finally {
            readLock.unlock();
        }
        return remoteServer;
    }

    @Override
    public void destroy() {
        if (null != zookeeperClient) {
            try {
                writeLock.lock();
                log.info("【{}】 shut down", serviceName);
                if (null != serverPoolMap && !serverPoolMap.isEmpty()) {
                    Iterator it = serverPoolMap.entrySet().iterator();
                    while (it.hasNext()) {
                        GenericObjectPool p = (GenericObjectPool) it.next();
                        if (null != p) {
                            p.close();
                        }
                        it.remove();
                    }
                }
                zookeeperClient.destroy();
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public ServerObject getObjectForRemote() {
        RemoteServer remoteServer = this.getUseRemote();
        if (null == remoteServer) {
            log.error("there is no server list serviceName={}, hostAndPorts={}, env={}", serviceName, hostAndPorts, env);
            return null;
        }
        String key = "";
        if (serverPoolMap.containsKey(key = createMapKey(remoteServer))) {
            GenericObjectPool<TTransport> pool = serverPoolMap.get(createMapKey(remoteServer));
            try {
                return new ServerObject(pool, remoteServer);
            } catch (Exception e) {
                log.error("borrowObject is wrong,the poll message is:", e);
                return null;
            }
        }

        GenericObjectPool<TTransport> pool = createGenericObjectPool(remoteServer);
        serverPoolMap.put(createMapKey(remoteServer), pool);
        try {
            return new ServerObject(pool, remoteServer);
        } catch (Exception e) {
            log.error("borrowObject is wrong,the poll message is:", e);
            return null;
        }
    }

    private void initZKClinet(String hostAndPorts, String serviceName, String env) {
        if (null == zookeeperClient) {
            zookeeperClient = new ZookeeperClient(env, hostAndPorts, serviceName, this);
            zookeeperClient.initZookeeper();
        }
    }

}
