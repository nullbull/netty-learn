package com.niu.netty.rpc.client;

import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.client.cluster.Icluster;
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

    private
    @Override
    public Object getObject() throws Exception {
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
