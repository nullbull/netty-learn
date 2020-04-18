package com.niu.netty.rpc.client;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.Icluster;
import com.niu.netty.rpc.client.cluster.impl.DirectClisterImpl;
import com.niu.netty.rpc.client.cluster.impl.RandomLoadBalancer;
import com.niu.netty.rpc.client.invoker.LocalMockInterceptor;
import com.niu.netty.rpc.generic.GenericService;
import com.niu.netty.rpc.protol.NiuBinaryProtocol;
import com.niu.netty.rpc.transport.TNiuFramedTransport;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TTransport;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 14:49
 * @desc:
 */
@Slf4j
@Data
public class NiuClientProxy implements FactoryBean<Object>, ApplicationContextAware, InitializingBean {


    public static final String ASYNC_IFACE = "AsyncIface";

    public static final String IFACE = "Iface";

    public static final String CLIENT = "Client";

    public static final String ASYNC_CLIENT = "AsyncClient";
    //请求体最大长度
    public static final int DEFAULT_MAXLENGTH = 10 * 1024 * 1024;
    //链接超时
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    //读取超时
    public static final int DEFAULT_READ_TIMOUT = 30 * 1000;

    private volatile CountDownLatch countDownLatch = new CountDownLatch(1);

    private String serviceInterface;

    private String zkPath;

    private String serverIPPorts;

    private Object NiuServerProxy;

    private ApplicationContext applicationContext;

    private boolean async = false;

    private boolean generic = false;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private int readTimeout = DEFAULT_READ_TIMOUT;

    private boolean retryRequest = true;

    private String localMockServiceImpl;

    private int retryTimes = 3;

    private GenericObjectPoolConfig genericObjectPoolConfig;

    private int maxTotal = 50;

    private int maxIdle = 20;

    private int minIdle = 10;

    private boolean lifo = true;

    private boolean fairness = false;

    private long maxWaitMillis = 30 * 1000;

    //多长时间运行一次
    private long timeBetweenEvictionRunsMillis = 3* 60 * 1000;

    private long minEvictableIdleTImeMillis = 5 * 60 * 1000;
    /*
     *  对象空闲多久后逐出, 当空闲时间>该值 且 空闲连接>最大空闲数 时直接逐出,
     *不再根据MinEvictableIdleTimeMillis判断  (默认逐出策略)
     */
    private long softMinEvictableIdleTimeMillis = 10 * 60 * 1000;

    private int numTestsPerEvictionRun = 20;

    private boolean testOnCreate = false;

    private boolean testOnBorrow = false;

    private boolean testOnReturn = false;

    private boolean testWhileIdle = true;

    private Icluster icluster;

    private ILoadBalancer loadBalancer;

    private String env = "env";

    AbandonedConfig abandonedConfig;

    private boolean removeAbandonedOnBorrow = true;

    private boolean removeAbandonedOnMaintenance = true;

    private int removeAbandonedTimeout = 30;

    private int maxLength = DEFAULT_MAXLENGTH;

    private static int cores = Runtime.getRuntime().availableProcessors();

    private int asyncSelectorThreadCount = cores * 2;

    private static volatile List<TAsyncClientManager> asyncClientManagerList = null;

    private String privateKey;

    private String publicKey;

    private boolean cat = false;

    private boolean isCat() { return cat;}

    private Class<?> genericAsyncIface;

    private Class<?> asyncIface;
    @Override
    public Object getObject() throws Exception {
        if (null == getNiuServerProxy()) {
            throw new RuntimeException("the Proxy can't be null");
        }
        return getNiuServerProxy();
     }

    @Override
    public Class<?> getObjectType() {
        if (null == serviceInterface) {
            return null;
        }
        return getIfaceInterface();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == serviceInterface) {
            throw new IllegalArgumentException("serviceInterface can't be null");
        }
        if (null == zkPath && null == serverIPPorts) {
            throw new IllegalArgumentException("zkPath or serverIpPorts at least ones can't be null");
        }
        Class<?> interfacee = null;
        if (null != localMockServiceImpl && !StringUtils.isEmpty(localMockServiceImpl.trim())) {
            LocalMockInterceptor localMockInterceptor = new LocalMockInterceptor(localMockServiceImpl);
            interfacee = getIfaceInterface();
            ProxyFactory pf = new ProxyFactory(interfacee, localMockInterceptor);
            setNiuServerProxy(pf.getProxy());
            return;
        }
        genericObjectPoolConfig = getGenericObjectPoolConfig();
        abandonedConfig = getAbandonedConfig();

        if (!StringUtils.isEmpty(serverIPPorts)) {
            icluster = new DirectClisterImpl(serverIPPorts, null == loadBalancer ? new RandomLoadBalancer() : loadBalancer, serviceInterface, async, connectionTimeout, readTimeout, genericObjectPoolConfig, abandonedConfig);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Class<?> getIfaceInterface() {
        if (async) {
            return getAsyncIfaceInterface();
        }
        return getSyncIfaceInterface();
    }
    private Class<?> getAsyncIfaceInterface() {
        Class<?>[] classes = null;
        if (!generic) {
            if (null != asyncIface) {
                return asyncIface;
            }
            try {
                classes = this.getClass().getClassLoader().loadClass(serviceInterface).getClasses();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("can't find the class :" + serviceInterface);
            }
        } else {
            if (null != genericAsyncIface){
                return genericAsyncIface;
            }
            classes = GenericService.class.getClasses();
        }
        for (Class c : classes) {
            if (c.isMemberClass() && c.isInterface() && c.getSimpleName().equals(ASYNC_IFACE)) {
                if (!generic) {
                    asyncIface = c;
                }else {
                    genericAsyncIface = c;
                }
                return c;
            }
        }
        throw new IllegalArgumentException ( "can't find the interface AsyncIface,please make the service with thrift tools!" );
    }

    private Class<?> genericSynIface;
    private Class<?> synIface;

    private Class<?> getSyncIfaceInterface() {
        Class<?>[] classes = null;

        if (!generic) {
            if (null != synIface) {
                return synIface;
            }
            try {
                classes = this.getClass().getClassLoader().loadClass(serviceInterface).getClasses();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("can't find the class:" + serviceInterface);
            }
        } else {
            if (null != genericSynIface) {
                return genericAsyncIface;
            }
            classes = GenericService.class.getClasses();
        }
        for (Class c : classes) {
            if (c.isMemberClass() && c.isInterface() && c.getSimpleName().equals(IFACE)) {
                if (!generic) {
                    synIface = c;
                } else {
                    genericSynIface = c;
                }
            }
            return c;
        }
        throw new IllegalArgumentException ( "can't find the interface Iface,please make the service with thrift tools" );
    }
    private Class<?> genericSynClient;

    private Class<?> synClient;

    private Class<?> getSynClientClass() {
        Class<?>[] classes = null;
        if (!generic) {
            if (null != synClient) {
                return synClient;
            }
            try {
                classes = this.getClass().getClassLoader().loadClass(serviceInterface).getClasses();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("can't find the class:" + serviceInterface);
            }
        } else {
            if (null != genericSynClient) {
                return genericSynClient;
            }
            classes = GenericService.class.getClasses();
        }
        for (Class c : classes) {
            if (c.isMemberClass() && !c.isInterface() && c.getSimpleName().equals(CLIENT)) {
                if (!generic) {
                    synClient = c;
                } else {
                    genericSynClient = c;
                }
                return c;
            }
        }
        throw new IllegalArgumentException ( "serviceInterface must contain Sub Class of Client" );
    }

    private Class<?> genericAsyncClient;

    private Class<?> asyncClient;

    private Class<?> getAsyncClientClass() {
        Class<?>[] classes = null;

        if (!generic) {
            try {
                if (null != asyncClient) {
                    return asyncClient;
                }
                classes = this.getClass().getClassLoader().loadClass(serviceInterface).getClasses();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("cant find the class :" + serviceInterface);
            }
        } else {
            if (null != genericAsyncClient) {
                return genericAsyncClient;
            }
            classes = GenericService.class.getClasses();
        }
        for (Class c : classes) {
            if (c.isMemberClass() && !c.isInterface() && c.getSimpleName().equals(ASYNC_CLIENT)) {
                if (!generic) {
                    genericAsyncClient = c;
                } else {
                    asyncClient = c;
                }
                return c;
            }
        }
        throw new IllegalArgumentException("serviceInterface must contain Sub Class of AsyncClient");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private Constructor<?> synConstructor;

    private Constructor<?> asynConstructor;

    public Object getInterfaceClientInstance(TTransport socket, String server)  {
        if (!async) {
            Class<?> clazz = getSynClientClass();
            try {
                if (null == synConstructor) {
                    synConstructor = clazz.getDeclaredConstructor(TProtocol.class);
                }
                TTransport tTransport = new TNiuFramedTransport(socket, maxLength);
                if (this.getPrivateKey() != null && null != this.getPublicKey()) {
                    ((TNiuFramedTransport) tTransport).setRsa((byte) 1);
                    ((TNiuFramedTransport) tTransport).setPrivateKey(this.getPrivateKey());
                    ((TNiuFramedTransport) tTransport).setPublicKey(this.getPublicKey());
                }
                TProtocol protocol = new NiuBinaryProtocol(tTransport);
                ((NiuBinaryProtocol) protocol).setGeneric(generic);
                return synConstructor.newInstance(protocol);
            } catch (NoSuchMethodException e) {
                log.error ( "the clazz can't find the Constructor with TProtocol.class" );
            } catch (InstantiationException e) {
                log.error ( "get InstantiationException", e );
            } catch (IllegalAccessException e) {
                log.error ( "get IllegalAccessException", e );
            } catch (InvocationTargetException e) {
                log.error ( "get InvocationTargetException", e );
            }
        } else {
            if (null == asyncClientManagerList) {
                synchronized (this) {
                    if (null == asyncClientManagerList) {
                        asyncClientManagerList = new ArrayList<>();
                        for (int i = 0; i < asyncSelectorThreadCount; i++) {
                            try {
                                asyncClientManagerList.add(new TAsyncClientManager());
                                countDownLatch.countDown();
                            }  catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (countDownLatch.getCount() > 0) {
                try {
                    countDownLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn ( "InterruptedException at wait  for other add TAsyncClientManager class:"+serviceInterface, e );
                }
            }
            Class<?> clazz = getAsyncClientClass();
            if (null == asynConstructor) {
                try {
                    asynConstructor = clazz.getDeclaredConstructor(TProcessorFactory.class, TAsyncClientManager.class, TNonblockingTransport.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            try {
                return asynConstructor.newInstance(new NiuBinaryProtocol.Factory(generic), asyncClientManagerList.get(socket.hashCode() % asyncSelectorThreadCount), socket);
            } catch (InstantiationException e) {
                log.error ( "get InstantiationException", e );
            } catch (IllegalAccessException e) {
                log.error ( "get IllegalAccessException", e );
            } catch (InvocationTargetException e) {
                log.error ( "get InvocationTargetException", e );
            }
         }
        return null;
    }

//    private Class<?> genericAsyncIface;
//    private Class<?> asyncIface;
//
//    private Class<?> getAsyncIfaceInterface() {
//        Class<?>[] classes = null;
//
//        if (!generic) {
//            if (asyncIface != null) return asyncIface;
//            try {
//                classes = this.getClass().getClassLoader().loadClass( serviceInterface).getClasses();
//            } catch (ClassNotFoundException e) {
//                throw new IllegalArgumentException("can not fin the class: " + serviceInterface);
//            }
//        } else {
//            if (genericAsyncIface != null) return genericAsyncIface;
//            classes = GenericService.class.getClasses();
//        }
//        for (Class c : classes) {
//            if (c.isMemberClass() && c.isInterface() && c.getSimpleName().equals(ASYNC_IFACE)) {
//                if (!generic) {
//                    asyncIface = c;
//                } else {
//                    genericAsyncIface = c;
//                }
//                return c;
//            }
//        }
//        throw new IllegalArgumentException ( "can't find the interface AsyncIface,please make the service with thrift tools!" );
//    }

    private AbandonedConfig getAbandonedConfig() {
        AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnBorrow(isRemoveAbandonedOnBorrow());
        abandonedConfig.setRemoveAbandonedOnMaintenance(isRemoveAbandonedOnMaintenance());
        abandonedConfig.setRemoveAbandonedTimeout(getRemoveAbandonedTimeout());
        return abandonedConfig;
    }
    private GenericObjectPoolConfig genericObjectPoolConfig() {
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(getMaxIdle());
        genericObjectPoolConfig.setMinIdle(getMinIdle());
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMaxWaitMillis(getMaxWaitMillis());
        genericObjectPoolConfig.setLifo(isLifo());
        genericObjectPoolConfig.setFairness(isFairness());
        genericObjectPoolConfig.setMinEvictableIdleTimeMillis(getMinEvictableIdleTImeMillis());
        genericObjectPoolConfig.setSoftMinEvictableIdleTimeMillis(getSoftMinEvictableIdleTimeMillis());
        genericObjectPoolConfig.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
        genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
        genericObjectPoolConfig.setTestOnCreate(isTestOnCreate());
        genericObjectPoolConfig.setTestOnBorrow(isTestOnBorrow());
        genericObjectPoolConfig.setTestWhileIdle(isTestWhileIdle());
        return genericObjectPoolConfig;
    }


    public void destory() {
        if (icluster != null) icluster.destroy();
    }

}
