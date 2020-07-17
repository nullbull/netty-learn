package com.niu.netty.rpc.server.config;

import com.niu.netty.rpc.generic.GenericRequest;
import com.niu.netty.rpc.generic.GenericService;
import com.niu.netty.rpc.generic.GenericServiceImpl;
import com.niu.netty.rpc.server.INiuServer;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
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
        Class clazz = getSysIfaceInterface(serviceInterface);
        try {
            return getProcessorClass(serviceInterface).getDeclaredConstructor(clazz).newInstance(serviceInterface);
        } catch (NoSuchMethodException e) {
        log.error ( "can't find the GenericTProcessor Constructor with Iface",e );
        } catch (IllegalAccessException e) {
                log.error ( "IllegalAccessException the GenericTProcessor with Iface" );
        } catch (InstantiationException e) {
                log.error ( "IllegalInstantiationExceptionAccessException the GenericTProcessor with Iface",e );
        } catch (InvocationTargetException e) {
                log.error ( "InvocationTargetException the GenericTProcessor with Iface",e );
        }
        return null;
    }

    public TProcessor getGenericTProcessor() {
        Class clazz = getSysIfaceInterface(GenericService.class);
        try {
            return getProcessorClass(GenericService.class).getDeclaredConstructor(clazz).newInstance(new GenericServiceImpl(serviceImpl));
        } catch (NoSuchMethodException e) {
            log.error ( "can't find the GenericTProcessor Constructor with Iface",e );
        } catch (IllegalAccessException e) {
            log.error ( "IllegalAccessException the GenericTProcessor with Iface" );
        } catch (InstantiationException e) {
            log.error ( "IllegalInstantiationExceptionAccessException the GenericTProcessor with Iface",e );
        } catch (InvocationTargetException e) {
            log.error ( "InvocationTargetException the GenericTProcessor with Iface",e );
        }
        return null;
    }


    private Class<?> getSysIfaceInterface(Class<?> serviceInterface){
        Class<?>[] clazzs = serviceInterface.getInterfaces();
        return Arrays.stream(clazzs).filter(c -> c.isMemberClass() && !c.isInterface() && c.getSimpleName().contentEquals(IFACE))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("serviceInterface must contain Sub Interface of Iface"));
    }
    private Class<TProcessor> getProcessorClass(Class<?> serviceInterface) {
        Class<?>[] clazzs = serviceInterface.getClasses();
        return Arrays.stream(clazzs).filter(c -> c.isMemberClass() && !c.isInterface() && c.getSimpleName().contentEquals(PROCESSOR))
                .findFirst()
                .map(c -> (Class<TProcessor>)c)
                .orElseThrow(() -> new IllegalArgumentException("serviceInterface must contain Sub Interface of Iface"));
    }

    protected void checkParam(){
        if (null == serviceImpl) {
            throw new IllegalArgumentException("this serviceImpl can't be null");
        }
        if(null == serviceInterface){
            throw new IllegalArgumentException ( "the serviceInterface can't be null" );
        }
        if(0 == port){
            throw new IllegalArgumentException ( "set the right port" );
        }
    }

}
