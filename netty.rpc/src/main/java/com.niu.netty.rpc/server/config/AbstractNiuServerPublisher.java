package com.niu.netty.rpc.server.config;

import com.niu.netty.rpc.server.INiuServer;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 15:52
 * @desc:
 */
@Slf4j
@Data
public class AbstractNiuServerPublisher {

    public static final int DEFAULT_EVENT_LOOP_THREADS;
    public static final int DEFAULT_NIU_THREADS;
    public static final int DEFAULT_THRIFT_ACCEPT_THREAD;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));
        if (log.isDebugEnabled()) {
            log.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
        DEFAULT_NIU_THREADS = 256;
        DEFAULT_THRIFT_ACCEPT_THREAD = 5;
    }
    public static final String IFACE = "Iface";
    public static final String PROCESSOR = "Processor";

    public Object serviceImpl;
    public Class<?> serviceInterface;
    public int port;
    public String zkPath;

    public int bossThreadCount;
    public int workThreadCount;
    public int niuThreadCount;
    public String env = "env";
    public int weight = 10;
    public String serverType = "NETTY";
    public int workQueue;


    public String privateKey;
    public String publicKey;
    public int maxLength = Integer.MAX_VALUE;
    public ApplicationContext applicationContext;
    public INiuServer niuServer;


    public TProcessor getTProcessor() {
        Class clazz = getSysIfaceInterface(Gener)
    }


    private Class<?> getSysIfaceInterface(Class<?> serviceInterface){
        Class<?>[] clazzs = serviceInterface.getInterfaces();
        return Arrays.stream(clazzs).filter(c -> c.isMemberClass() && !c.isInterface() && c.getSimpleName().contentEquals(IFACE))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("serviceInterface must contain Sub Interface of Iface"));
    }

}
