package com.niu.netty.rpc.client.cluster.impl;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.RemoteServer;
import com.niu.netty.rpc.client.cluster.ServerObject;
import com.niu.netty.rpc.utils.NiuRegexUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 17:21
 * @desc:
 */
@Slf4j
public class DirectClisterImpl extends AbstractBaseIcluster {

    public static final String REGEX = "[^0-9a-zA-Z_\\-\\.:#]+";
    public static final String REGEX_IPS = "[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}:[0-9]{1,5}#[0-9]{1,}[,]{0,}]+";

    private String hostAndPorts;

    private ILoadBalancer loadBalancer;

    private String serviceName;

    private List<RemoteServer> serverList = new ArrayList<>();
    public DirectClisterImpl(String hostAndPorts, ILoadBalancer loadBalancer, String serviceName, boolean async, int connectionTimeout, int soTimeout, GenericObjectPoolConfig genericObjectPoolConfig, AbandonedConfig abandonedConfig) {
        super(loadBalancer, serviceName, async, connectionTimeout, soTimeout, genericObjectPoolConfig, abandonedConfig);
        this.hostAndPorts = hostAndPorts;
        this.loadBalancer = loadBalancer;
        this.serviceName = serviceName;
        if (!NiuRegexUtil.match(REGEX_IPS, hostAndPorts)) {
            throw new RuntimeException("error hostAndPorts:" + hostAndPorts + ",serviceName:" + serviceName);
        }
    }

    @Override
    public RemoteServer getUseRemote() {
        if (serverList.isEmpty()) {
            if (null == this.hostAndPorts) {
                return null;
            }
            String[] array = hostAndPorts.split(REGEX);
            List<RemoteServer> list = new ArrayList<>();
            Arrays.stream(array).forEach(
                    temp -> {
                        String hostAndIp = temp.split("#")[0].trim();
                        Integer weight = Integer.valueOf(temp.split(":")[1].trim());
                        String host = hostAndIp.split(":")[0].trim();
                        String port = hostAndIp.split(":")[1].trim();
                        String server = StringUtils.EMPTY;
                        list.add(new RemoteServer(host, port, weight, true, server));
                    }
            );
            serverList = list;
        }
        return loadBalancer.select(serverList);
    }

    @Override
    public void destroy() {
        log.info("[{}] shut down", serviceName);
        serverList = null;
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
    }

    @Override
    public ServerObject getObjectForRemote() {
        RemoteServer remoteServer = this.getUseRemote();
        if (null == remoteServer) {
            return null;
        }
        String key = "";
        if (serverPoolMap.contains(key = createMapKey(remoteServer))) {
            GenericObjectPool<TTransport> pool = serverPoolMap.get(key);
            try {
                return new ServerObject(pool, remoteServer);
            } catch (Exception e) {
                log.error("borrow Object was wrong, the pool message is:", e);
                return null;
            }
        }
        GenericObjectPool<TTransport> pool = createGenericObjectPool(remoteServer);
        serverPoolMap.put(key, pool);
        return new ServerObject(pool, remoteServer);
    }
}
