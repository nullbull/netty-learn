package com.niu.netty.rpc.client.cluster;

import java.rmi.server.RemoteServer;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 15:47
 * @desc:
 */
public interface Icluster {

    RemoteServer getUseRemote();

    void destroy();

    ServerObject getObjectForRemote();
}
