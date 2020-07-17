package com.niu.netty.rpc.parser;

import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.server.NiuServerPublisher;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author niuzhenhao
 * @date 2020/7/17 17:01
 * @desc
 */

public class NiuBeanHandler extends NamespaceHandlerSupport {


    private static final String CLIENT = "client";

    private static final String SERVER = "server";

    private static final String ANNOTATION = "annotation";

    @Override
    public void init() {
        registerBeanDefinitionParser(CLIENT, new NiuBeanDefinitionParser(NiuClientProxy.class));
        registerBeanDefinitionParser(SERVER, new NiuBeanDefinitionParser(NiuServerPublisher.class));
        registerBeanDefinitionParser(ANNOTATION, new NiuBeanDefinitionParser(NiuAnnotationBean.class));
    }
}
