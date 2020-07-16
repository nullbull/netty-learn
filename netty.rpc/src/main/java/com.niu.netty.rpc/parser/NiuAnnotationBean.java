package com.niu.netty.rpc.parser;

import com.niu.netty.rpc.annotation.NiuServer;
import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.server.NiuServerPublisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
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
    public Object postProcessBeforeInitialization(Object object, String beanName) throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }



        //todo 2020-07-16
    }



    @Override
    public int getOrder() {
        return 0;
    }
}
