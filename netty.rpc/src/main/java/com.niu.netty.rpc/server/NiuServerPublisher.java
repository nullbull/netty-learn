package com.niu.netty.rpc.server;

import com.niu.netty.rpc.netty.NettyServer;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import com.niu.netty.rpc.thrift.ThriftServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 15:49
 * @desc:
 */
@Slf4j
public class NiuServerPublisher extends AbstractNiuServerPublisher implements FactoryBean<Object>, ApplicationContextAware, InitializingBean {
    private static final String NETTY = "com.niu.netty.rpc/netty";
    private static final String THRIFT = "com.niu.netty.rpc/thrift";

    @Override
    public Object getObject() throws Exception {
        return this;
    }

    @Override
    public Class<?> getObjectType() {
        return this.getClass();
    }
    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet()  {
        this.checkParam();
        if (NETTY.endsWith(this.serverType.toLowerCase().trim())) {
            niuServer = new NettyServer(this);
        } else if (THRIFT.equals(this.serverType.toLowerCase().trim())) {
            niuServer = new ThriftServer(this);
        } else {
            log.error("other server is not support at since v1.0,className:{}", getServiceInterface().getName());
            throw new IllegalArgumentException("other server is not support at since v1.0,className=" + getServiceInterface().getName());
        }
        niuServer.run();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void destroy() {
        if (this.niuServer != null) {
            this.niuServer.stop();
        }
    }
}
