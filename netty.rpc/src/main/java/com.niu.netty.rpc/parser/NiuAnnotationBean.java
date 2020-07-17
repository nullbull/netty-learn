package com.niu.netty.rpc.parser;

import com.niu.netty.rpc.annotation.NiuClient;
import com.niu.netty.rpc.annotation.NiuServer;
import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.client.cluster.ILoadBalancer;
import com.niu.netty.rpc.server.NiuServerPublisher;
import com.niu.netty.rpc.utils.NiuAopUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class NiuAnnotationBean implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, BeanFactoryAware, Ordered {
    public static final String COMMA_SPLIT_PATTERN = "\\s*[,]+\\s*";

    private String annotationPackage;

    private String[] annotationPackages;

    private BeanFactory beanFactory;

    private final Set<NiuServerPublisher> niuServerPublishers = new HashSet<>();
    private final Map<String, NiuClientProxy> niuClientProxyMap = new ConcurrentHashMap<>();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    @Override
    public void destroy() throws Exception {
        for (NiuClientProxy clientProxy : niuClientProxyMap.values()) {
            clientProxy.destory();
        }
        for (NiuServerPublisher publisher : niuServerPublishers) {
            publisher.destroy();
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        if (annotationPackage == null || annotationPackage.length() == 0) {
            return;
        }
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                Class<?> scannerClass = ClassUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner", NiuAnnotationBean.class.getClassLoader());
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class});

                Class<?> filterClass = ClassUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter", NiuAnnotationBean.class.getClassLoader());

                Object filter = filterClass.getConstructor(Class.class).newInstance(NiuServer.class);

                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter", ClassUtils.forName("org.springframework.core.type.filter.TypeFilter", NiuAnnotationBean.class.getClassLoader()));

                addIncludeFilter.invoke(scanner, filter);

                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
                scan.invoke(scanner, new Object[]{annotationPackages});
            } catch (Throwable e) {

            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Class<?> clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }

        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            String name = method.getName();
            int modifiers = method.getModifiers();
            if (name.length() > 3 && name.startsWith("set") && method.getParameterTypes().length == 1 && Modifier.isProtected(modifiers) && !Modifier.isStatic(modifiers)) {
                try {
                    NiuClient niuClient = method.getAnnotation(NiuClient.class);
                    if (niuClient != null) {
                        Object value = getClientInvoke(niuClient, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                        }
                    }
                } catch (Exception e) {
                    throw new BeanInitializationException("Failed to init remote service reference at method " + name + " in class " + bean.getClass().getName(), e);
                }
            }
        }
        return bean;
    }

    private Object getClientInvoke(NiuClient niuClient, Class<?> beanClass) {
        NiuClientProxy niuClientProxy = new NiuClientProxy();

        boolean async;

        String interfaceName = beanClass.getName();

        if (interfaceName.endsWith("$Iface")) {
            niuClientProxy.setAsync(false);
            async = false;
        } else if (interfaceName.endsWith("$AsyncIface")) {
            niuClientProxy.setAsync(true);
            async = true;
        } else {
            throw new RuntimeException("the bean :" + beanClass + "not allow with annotation @NiuClient");
        }

        if (StringUtils.isEmpty(niuClient.genericService())) {
            if (niuClientProxyMap.containsKey(interfaceName)) {
                return niuClientProxyMap.get(interfaceName).getObject();
            }
        } else {
            String key = "generic-".concat(async ? "async-" : "sync-").concat(niuClient.genericService());
            if (niuClientProxyMap.containsKey(key)) {
                return niuClientProxyMap.get(key).getObject();
            }
        }

        if (StringUtils.isNoneBlank(niuClient.genericService())) {
            niuClientProxy.setGeneric(true);
            niuClientProxy.setServiceInterface(niuClient.genericService());
        } else {
            niuClientProxy.setGeneric(false);
            niuClientProxy.setServiceInterface(beanClass.getDeclaringClass().getName());
        }

        if (StringUtils.isNotEmpty(niuClient.zkPath())) {
            niuClientProxy.setZkPath(niuClient.zkPath());
        }

        if (StringUtils.isNotEmpty(niuClient.serverIpPorts())) {
            niuClientProxy.setServerIPPorts(niuClient.serverIpPorts());
        }

        niuClientProxy.setConnectionTimeout(niuClient.connectionTimeout());

        niuClientProxy.setReadTimeout(niuClient.readTimeout());

        if (StringUtils.isNotEmpty(niuClient.localMockServiceImple())) {
            niuClientProxy.setLocalMockServiceImpl(niuClient.localMockServiceImple());
        }

        niuClientProxy.setRetryRequest(niuClient.retryRequest());
        niuClientProxy.setRetryTimes(niuClient.retryTimes());
        niuClientProxy.setMaxTotal(niuClient.maxTotal());
        niuClientProxy.setMaxIdle(niuClient.maxIdle());
        niuClientProxy.setMinIdle(niuClient.minIdle());
        niuClientProxy.setLifo(niuClient.lifo());
        niuClientProxy.setFairness(niuClient.fairness());
        niuClientProxy.setMaxWaitMillis(niuClient.maxWaitMillis());
        niuClientProxy.setTimeBetweenEvictionRunsMillis(niuClient.timeBetweenEvictionRunsMillis());
        niuClientProxy.setMinEvictableIdleTImeMillis(niuClient.minEvictableIdleTimeMillis());
        niuClientProxy.setSoftMinEvictableIdleTimeMillis(niuClient.softMinEvictableIdleTimeMillis());
        niuClientProxy.setNumTestsPerEvictionRun(niuClient.numTestsPerEvictionRun());
        niuClientProxy.setTestOnCreate(niuClient.testOnCreate());
        niuClientProxy.setTestOnBorrow(niuClient.testOnBorrow());
        niuClientProxy.setTestOnReturn(niuClient.testOnReturn());
        niuClientProxy.setTestWhileIdle(niuClient.testWhileIdle());

        String iLoadBalancer = niuClient.iLoadBalancer();
        if (StringUtils.isNotEmpty(iLoadBalancer)) {
            Object o = beanFactory.getAliases(iLoadBalancer);
            niuClientProxy.setLoadBalancer((ILoadBalancer) o);
        }

        niuClientProxy.setEnv(niuClient.env());
        niuClientProxy.setRemoveAbandonedOnBorrow(niuClient.removeAbandonedOnBorrow());
        niuClientProxy.setRemoveAbandonedOnMaintenance(niuClient.removeAbandonedOnMaintenance());
        niuClientProxy.setRemoveAbandonedTimeout(niuClient.removeAbandonedTimeout());
        niuClientProxy.setMaxLength(niuClient.maxLength_());

        if (StringUtils.isNotEmpty(niuClient.privateKey()) && StringUtils.isNotEmpty(niuClient.publicKey())) {
            niuClientProxy.setPrivateKey(niuClient.privateKey());
            niuClientProxy.setPublicKey(niuClient.publicKey());
        }

        niuClientProxy.afterPropertiesSet();

        if (StringUtils.isEmpty(niuClient.genericService())) {
            niuClientProxyMap.put(interfaceName, niuClientProxy);
        } else {
            String key = "generic-".concat(async ? "async-" : "sync-").concat(niuClient.genericService());
            niuClientProxyMap.put(key, niuClientProxy);
        }

        return niuClientProxy.getObject();

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Class<?> clazz = bean.getClass();

        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }

        NiuServer niuServer = clazz.getAnnotation(NiuServer.class);

        if (niuServer != null) {
            NiuServerPublisher niuServerPublisher = new NiuServerPublisher();
            niuServerPublisher.setPort(niuServer.port());
            niuServerPublisher.setServiceImpl(bean);

            if (!StringUtils.isEmpty(niuServer.zkpath())) {
                niuServerPublisher.setZkPath(niuServer.zkpath());
            }

            if (niuServer.bossThreadCount() != 0) {
                niuServerPublisher.setBossThreadCount(niuServer.bossThreadCount());
            }

            if (niuServer.workThreadCount() != 0) {
                niuServerPublisher.setWorkThreadCount(niuServer.workThreadCount());
            }

            if (niuServer.niuThreadCount() != 0) {
                niuServerPublisher.setNiuThreadCount(niuServer.niuThreadCount());
            }
            niuServerPublisher.setMaxLength(niuServer.maxLength());
            niuServerPublisher.setEnv(niuServer.env());
            niuServerPublisher.setWeight(niuServer.weight());
            niuServerPublisher.setServerType(niuServer.serverType());

            if (niuServer.workQueue() != 0) {
                niuServerPublisher.setWorkQueue(niuServer.workQueue());
            }

            if (!StringUtils.isEmpty(niuServer.privateKey()) && !StringUtils.isEmpty(niuServer.publicKey())) {
                niuServerPublisher.setPrivateKey(niuServer.privateKey());
                niuServerPublisher.setPublicKey(niuServer.publicKey());
            }

            Class<?> serviceInterface = null;

            Object targetBean = NiuAopUtil.getTarget(bean);
            if (targetBean == null) {
                throw new BeanInitializationException("bean :" + bean.getClass().getName() + "getTarget class error");
            }

            Class<?>[] serviceInterfaces = targetBean.getClass().getInterfaces();
            for (Class<?> clazz1 : serviceInterfaces) {
                if (clazz1.getName().endsWith("$Iface")) {
                    serviceInterface = clazz1.getDeclaringClass();
                    break;
                }

                if (serviceInterface == null) {
                    throw new BeanInitializationException("bean :" + bean.getClass().getName() + "is not the thrift interface parent class");
                }
                niuServerPublisher.setServiceInterface(serviceInterface);
                niuServerPublisher.afterPropertiesSet();
                niuServerPublishers.add(niuServerPublisher);
            }
        }
        return bean;
    }


    private boolean isMatchPackage(Object bean) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        Class clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        String beanClassName = clazz.getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    public void setAnnotationPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : annotationPackage.split(COMMA_SPLIT_PATTERN);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
