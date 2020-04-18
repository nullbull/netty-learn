package com.niu.netty.rpc.poolfactory;

import com.niu.netty.rpc.client.cluster.RemoteServer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 16:05
 * @desc:
 */
@Slf4j
@AllArgsConstructor
public class NiuPoolableObjectFactory extends BasePooledObjectFactory<TTransport> {
    private RemoteServer remoteServer;

    private int timeOut;

    private int connectionTimeOut;

    private boolean async;

    @Override
    public TTransport create() throws Exception {
       int count = 3;
        TTransportException exception = null;
        while (count-- > 0) {
            Instant start = Instant.now();
            TTransport tTransport = null;
            boolean connectSuccess = false;
            try {
                if (async) {
                    tTransport = new TNonblockingSocket(remoteServer.getIp(), Integer.valueOf(remoteServer.getPort()), this.connectionTimeOut);
                    log.debug("makeObject() " + ((TNonblockingSocket)tTransport).getSocketChannel().socket());
                } else {
                    tTransport = new TSocket(remoteServer.getIp(), Integer.valueOf(remoteServer.getPort()), this.connectionTimeOut);
                    tTransport.open();
                    ((TSocket)tTransport).setTimeout(timeOut);
                    log.debug("makeObject() " + ((TSocket) tTransport).getSocket());
                }
                connectSuccess = true;
                return tTransport;
            } catch (TTransportException e) {
                exception = e;
                log.warn(new StringBuilder("makeObject() ").append(e.getLocalizedMessage())
                .append(":")
                .append(e.getType())
                .append("/")
                .append(remoteServer.getIp())
                .append(":")
                .append(remoteServer.getPort())
                .append("/")
                .append(Duration.between(Instant.now(), start).toMillis())
                .toString());
                if (!(e.getCause() instanceof SocketTimeoutException)) {
                    break;
                }
            } catch (Exception e) {
                log.warn("makeObject()", e);
                throw new RuntimeException(e);
            } finally {
                if (null != tTransport && connectSuccess == false) {
                    try {
                        tTransport.close();
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            }

        }
        throw new RuntimeException(exception);
    }

    @Override
    public PooledObject<TTransport> wrap(TTransport tTransport) {
        return new DefaultPooledObject<>(tTransport);
    }

    @Override
    public void destroyObject(PooledObject<TTransport> p) throws Exception {
        TTransport tTransport = p.getObject();
        if (tTransport instanceof TSocket) {
            TSocket socket = (TSocket) tTransport;
            if (socket.isOpen()) {
                log.debug("destroy() host:" + remoteServer.getIp() + ",port:" + remoteServer.getPort()
                          + ",socket:" + socket.getSocket() + ",isOpen:" + socket.isOpen());
            }
        } else if (tTransport instanceof  TNonblockingSocket) {
            TNonblockingSocket socket = (TNonblockingSocket) tTransport;
            if (socket.getSocketChannel().isOpen()) {
                log.debug("destroyObject() host:" + remoteServer.getIp() + ",port:" + remoteServer.getPort()
                         + ",isOpen:" + socket.isOpen());
                socket.close();
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<TTransport> p) {
        TTransport tTransport = p.getObject();

        try {
            if (tTransport instanceof TSocket) {
                TSocket thriftSocket = (TSocket) tTransport;
                if (thriftSocket.isOpen()) {
                    return true;
                } else {
                    log.warn("validateObject() failed" + thriftSocket.getSocket());
                    return false;
                }
            } else if (tTransport instanceof TNonblockingSocket) {
                TNonblockingSocket socket = (TNonblockingSocket) tTransport;
                if (socket.getSocketChannel().isOpen()) {
                    return true;
                } else {
                    log.warn("validateObject() failde " + socket.getSocketChannel().socket());
                    return false;
                }
            } else {
                log.warn("validateObject() failed unknown Object: " + tTransport);
                return false;
            }
        } catch (Exception e) {
            log.warn("validateObject() failed " + e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<TTransport> p) throws Exception {
        log.debug("activateObject: PooledObject:【{}】", p);
    }

    @Override
    public void passivateObject(PooledObject<TTransport> p)
            throws Exception {
        log.debug ( "passivateObject:PooledObject:【{}】",p );
    }
}
