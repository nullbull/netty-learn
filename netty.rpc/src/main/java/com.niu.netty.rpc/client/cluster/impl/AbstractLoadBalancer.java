package com.niu.netty.rpc.client.cluster.impl;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.RemoteServer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 17:08
 * @desc:
 */
public abstract class AbstractLoadBalancer implements ILoadBalancer {

    protected int getWeight(RemoteServer remoteServer) {
        if (null != remoteServer && remoteServer.isEnable()) {
            return remoteServer.getWeight();
        }
        return -1;
    }
    @Override
    public RemoteServer select(List<RemoteServer> list) {
        if (null == list) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }

        List<RemoteServer> temp = new ArrayList<>();

        for (int i = list.size() - 1; i >= 0; i--) {
            RemoteServer remoteServer = list.get(i);
            if (remoteServer.isEnable()) {
                list.add(remoteServer);
            }
        }
        return doSelect(temp);
    }
    public abstract RemoteServer doSelect(List<RemoteServer> list);
}
