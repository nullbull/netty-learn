package com.niu.netty.rpc.client.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TTransport;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 15:48
 * @desc:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerObject {
    private GenericObjectPool<TTransport> genericObjectPool;
    private RemoteServer remoteServer;
}
