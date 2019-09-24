package com.niu.netty.rpc.client;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.Icluster;
import com.niu.netty.rpc.generic.GenericService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.async.TAsyncClientManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.concurrent.CountDownLatch;

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
        }
    }

}
