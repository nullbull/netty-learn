package com.niu.netty.rpc.client.cluster;

import java.util.List;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 15:55
 * @desc:
 */
public interface ILoadBalancer {

    RemoteServer select(List<RemoteServer> list);
}
