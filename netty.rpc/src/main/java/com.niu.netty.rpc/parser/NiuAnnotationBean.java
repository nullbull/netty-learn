package com.niu.netty.rpc.parser;

import com.niu.netty.rpc.annotation.NiuClient;
import com.niu.netty.rpc.annotation.NiuServer;
import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.server.NiuServerPublisher;
import com.niu.netty.rpc.utils.NiuAopUtil;
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
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
            if (name.length() > 3 && name.startsWith("set") && method.getParameterTypes().length == 1 && Modifier.isProtected(modifiers)) && !Modifier.isStatic(modifiers)) {
                try {
                    NiuClient niuClient = method.getAnnotation(NiuClient.class);
                    if (niuClient != null) {
                        Object value =
                    }
                }
            }
        }
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



    @Override
    public int getOrder() {
        return 0;
    }
}
