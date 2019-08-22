package com.niu.netty.rpc.client.cluster;

import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TTransport;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 15:48
 * @desc:
 */
@Data
public class ServerObject {
    private GenericObjectPool<TTransport> genericObjectPool;
    private RemoteServer remoteServer;
}
