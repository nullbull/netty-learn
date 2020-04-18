package com.niu.netty.rpc.parser;

import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.server.NiuServerPublisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

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
            publisher.
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }


    @Override
    public int getOrder() {
        return 0;
    }
}
