package com.niu.netty.rpc.client.invoker;


import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.client.async.ReleaseResourcesNiuAsyncCallBack;
import com.niu.netty.rpc.client.cluster.Icluster;
import com.niu.netty.rpc.client.cluster.ServerObject;
import com.niu.netty.rpc.exceptions.OutMaxLengthException;
import com.niu.netty.rpc.exceptions.RSAException;
import com.niu.netty.rpc.utils.TraceThreadContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * @author: niuzhenhao
 * @date: 2019-08-31 17:15
 * @desc:
 */

@Slf4j
@AllArgsConstructor
public class NiuMethodInterceptor implements MethodInterceptor {

    private Icluster icluster;
    private int retryTimes;
    private boolean retryRequest;
    private NiuClientProxy niuClientProxy;
    private int asyncTimeOut;
    private boolean cat;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        String methodName = method.getName();
        Object[] args = methodInvocation.getArguments();

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (Object.class.equals(method.getDeclaredAnnotations())) {
            try {
                return method.invoke(this, args);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage() + " className:" + niuClientProxy.getServiceInterface() + ",method:" + methodName);
                return null;
            }
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return this.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return this.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return this.equals(args[0]);
        }
        boolean serviceTop = false;

        try {
            TTransport socket = null;
            int currTryTimes = 0;
            while (currTryTimes ++ < retryTimes) {
                ServerObject serverObject = icluster.getObjectForRemote();
                if (null == serverObject) {
                    throw new TException("no server list to user :" + niuClientProxy.getServiceInterface());
                }
                GenericObjectPool<TTransport> genericObjectPool = serverObject.getGenericObjectPool();
                try {
                    long before = System.currentTimeMillis();
                    socket = genericObjectPool.borrowObject();
                    long after = System.currentTimeMillis();
                    log.debug("get Object from pool with {} ms" + " className:" + niuClientProxy.getServiceInterface() + ",method:" + methodName, after - before);
                } catch (Exception e) {
                    if (null != socket) {
                        genericObjectPool.returnObject(socket);
                    }
                    log.error(e.getMessage() + " className:" + niuClientProxy.getServiceInterface() + ",method:" + methodName, e);
                    throw new TException("borrowObject error :" + niuClientProxy.getServiceInterface());
                }
                Object object = niuClientProxy.getInterfaceClientInstance(socket, serverObject.getRemoteServer().getServer());
                if (object instanceof TAsyncClient) {
                    ((TAsyncClient) object).setTimeout(asyncTimeOut);
                    if (args.length < 1 ) {
                        genericObjectPool.returnObject(socket);
                        throw new TException("args number error, className:" + niuClientProxy.getServiceInterface() + ",method:" + methodName);
                    }
                    Object argsLast = args[args.length - 1];
                    if (!(argsLast instanceof AsyncMethodCallback)) {
                        genericObjectPool.returnObject(socket);
                        throw new TException("args type error,className:" + niuClientProxy.getServiceInterface() + ",method:" + methodName);
                    }
                    AsyncMethodCallback callback = (AsyncMethodCallback) argsLast;
                    ReleaseResourcesNiuAsyncCallBack releaseResourcesNiuAsyncCallBack = new ReleaseResourcesNiuAsyncCallBack(callback, serverObject, socket);
                    args[args.length - 1] = releaseResourcesNiuAsyncCallBack;
                }
                try {
                    Object o = method.invoke(object, args);
                    if (socket instanceof TSocket) {
                        genericObjectPool.returnObject(socket);
                    }
                    return o;
                } catch (Exception e) {
                    Throwable cause = (e.getCause () == null) ? e : e.getCause ();

                    if (cause instanceof TApplicationException) {
                        if (((TApplicationException) cause).getType () == 6666) {
                            log.info ( "serverName【{}】,method:【{}】 thread pool is busy ,retry it!,error message from server 【{}】", niuClientProxy.getServiceInterface () ,methodName,((TApplicationException) cause).getMessage ());
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            Thread.yield ();
                            if (retryRequest)
                                continue;
                            throw new TApplicationException ("serverName:" + niuClientProxy.getServiceInterface () + ",method:" +methodName +"error,thread pool is busy,error message:" + ((TApplicationException) cause).getMessage () );
                        }

                        if (((TApplicationException) cause).getType () == 9999) {
                            log.error ( "serverName【{}】,method:【{}】 ,error message from server 【{}】", niuClientProxy.getServiceInterface (),methodName,((TApplicationException) cause).getMessage ());
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            throw new RSAException ("server ras error,serverName:"+niuClientProxy.getServiceInterface ()+",method:"+methodName,cause);
                        }

                        if (((TApplicationException) cause).getType () == 6699) {
                            log.error ( "serverName【{}】,method:【{}】 ,this client is not rsa support,please get the privateKey and publickey ,error message from server【{}】", niuClientProxy.getServiceInterface (),methodName,((TApplicationException) cause).getMessage ());
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            throw new RSAException ("this client is not rsa support,please get the privateKey and publickey with server,serverName:"+niuClientProxy.getServiceInterface ()+",method:"+methodName,cause);
                        }

                        if (((TApplicationException) cause).getType () == TApplicationException.INTERNAL_ERROR) {
                            log.error ( "serverName【{}】,method:【{}】,the remote server process error:【{}】", niuClientProxy.getServiceInterface (),methodName,((TApplicationException) cause).getMessage () );
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            throw new TException ("server process error, serviceName:" + niuClientProxy.getServiceInterface () + ",method:" + methodName + ",error message:" + ((TApplicationException) cause).getMessage (),cause);
                        }

                        if (((TApplicationException) cause).getType () == TApplicationException.MISSING_RESULT) {
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            return null;
                        }
                    }

                    if (cause instanceof RSAException) {
                        log.error ( "this client privateKey or publicKey is error,please check it! ,serverName【{}】,method 【{}】", niuClientProxy.getServiceInterface (), methodName);
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        throw new RSAException("this client privateKey or publicKey is error,please check it!" + "serverName:"+niuClientProxy.getServiceInterface () + ",method:"+methodName);
                    }

                    if(cause instanceof OutMaxLengthException){
                        log.error ( (cause ).getMessage (),cause );
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        throw new OutMaxLengthException("to big content!" + "serverName:"+ niuClientProxy.getServiceInterface ()+ ",method:" + methodName,cause);
                    }

                    if (cause.getCause () != null && cause.getCause () instanceof ConnectException) {
                        log.error ( "the server【{}】 maybe is shutdown ,retry it! serverName【{}】,method 【{}】", serverObject.getRemoteServer (),niuClientProxy.getServiceInterface (),methodName );
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        if (retryRequest)
                            continue;
                        throw new TException ("serverName:" + niuClientProxy.getServiceInterface () + ",method:" +methodName +"error,maybe is shutdown,error message:" + ((TApplicationException) cause).getMessage () );
                    }

                    if (cause.getCause () != null && cause.getCause () instanceof SocketTimeoutException) {
                        log.error ( "read timeout SocketTimeoutException --serverName【{}】,method：【{}】",niuClientProxy.getServiceInterface () ,methodName);
                        if (socket != null) {
                            try {
                                genericObjectPool.invalidateObject ( socket );
                            } catch (Exception e1) {
                                log.error ( "invalidateObject error", e );
                                throw new TException(new IllegalStateException("SocketTimeout and invalidateObject error,className:" + niuClientProxy.getServiceInterface ()) + ",method:" + methodName );

                            }
                        }
                        throw new TException(new SocketTimeoutException("SocketTimeout by --serverName:"+ niuClientProxy.getServiceInterface () + ",method:"+methodName));
                    }

                    if(cause instanceof TTransportException){
                        if(((TTransportException) cause).getType () == TTransportException.END_OF_FILE){
                            log.error ( "TTransportException,END_OF_FILE! {}--serverName【{}】,method：【{}】", serverObject.getRemoteServer (),niuClientProxy.getServiceInterface (),methodName);
                            if (socket != null) {
                                try {
                                    genericObjectPool.invalidateObject ( socket );
                                } catch (Exception e1) {
                                    log.error ( "invalidateObject error", e );
                                    throw new TException ( new IllegalStateException("TTransportException.END_OF_FILE and invalidateObject error,className:" + niuClientProxy.getServiceInterface ()) + ",method:" + methodName );
                                }
                            }
                            throw new TException(new TTransportException("the remote server is shutdown!" + serverObject.getRemoteServer () + niuClientProxy.getServiceInterface ()));
                        }
                        if(cause.getCause ()!=null && cause.getCause () instanceof SocketException){
                            if (socket != null) {
                                try {
                                    genericObjectPool.invalidateObject ( socket );
                                } catch (Exception e1) {
                                    log.error ( "invalidateObject error", e );
                                    throw new TException ( new IllegalStateException("TTransportException cause by SocketException and invalidateObject error,className:" + niuClientProxy.getServiceInterface ()) + ",method:" + methodName );
                                }
                            }
                            if (retryRequest)
                                continue;
                            throw new TException(new TTransportException("the remote server is shutdown!" + serverObject.getRemoteServer () + niuClientProxy.getServiceInterface ()));
                        }
                    }

                    if(cause instanceof TBase){
                        log.warn ( "thrift exception--{}, {}--serverName【{}】,method：【{}】",cause.getClass ().getName (), serverObject.getRemoteServer (),niuClientProxy.getServiceInterface (),methodName);
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        throw cause;
                    }

                    if (socket != null)
                        genericObjectPool.invalidateObject ( socket );
                    log.error ( "invoke server error,server ip -【{}】,port -【{}】--serverName【{}】,method：【{}】", serverObject.getRemoteServer ().getIp (), serverObject.getRemoteServer ().getPort (),niuClientProxy.getServiceInterface (),methodName  );
                    throw cause;
                }
            }
            TException finallyException = new TException("error!retry time-out of:" + retryTimes + "!!! " +"serverName:" +niuClientProxy.getServiceInterface () + ",method:" + methodName);
            throw finallyException;
        } finally {
            if(serviceTop && cat){
                System.out.println (123);
                TraceThreadContext.remove ();
            }
        }
    }

}
