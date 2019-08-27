package com.niu.netty.rpc.client.cluster.impl;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.Icluster;
import com.niu.netty.rpc.client.cluster.RemoteServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 15:57
 * @desc:
 */
@Slf4j
public abstract class AbstractBaseIcluster implements Icluster {
    private ILoadBalancer loadBalancer;

    private String serviceName;

    private boolean async;

    private int connectionTimeout;

    private int soTimeout;

    private GenericObjectPoolConfig genericObjectPoolConfig;

    private AbandonedConfig abandonedConfig;

    public ConcurrentHashMap<String, GenericObjectPool<TTransport>> serverPoolMap = new ConcurrentHashMap<>();

    public AbstractBaseIcluster(ILoadBalancer loadBalancer, String serviceName, boolean async, int connectionTimeout, int soTimeout, GenericObjectPoolConfig genericObjectPoolConfig, AbandonedConfig abandonedConfig) {
        this.loadBalancer = loadBalancer;
        this.serviceName = serviceName;
        this.async = async;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.genericObjectPoolConfig = genericObjectPoolConfig;
        this.abandonedConfig = abandonedConfig;
    }

    protected GenericObjectPool<TTransport> createGenericObjectPool(RemoteServer remoteServer) {
        GenericObjectPool<TTransport> genericObjectPool = new GenericObjectPool<>(new N )
    }
}
