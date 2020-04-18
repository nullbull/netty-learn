package com.niu.netty.rpc.server;

import com.niu.netty.rpc.netty.NettyServer;
import com.niu.netty.rpc.server.config.AbstractNiuServerPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 15:49
 * @desc:
 */
@Slf4j
public class NiuServerPublisher extends AbstractNiuServerPublisher implements FactoryBean<Object>, ApplicationContextAware, InitializingBean {
    private static final String NETTY = "com.niu.netty.rpc/netty";
    private static final String THRIFT = "thrift";

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
    public void afterPropertiesSet() throws Exception {
        this.checkParam();
        if (NETTY.endsWith(this.serverType.toLowerCase().trim())) {
            niuServer = new NettyServer(this);
        } else if (THRIFT.equals(this.serverType.toLowerCase().trim())) {
            niuServer = new Thrift
        }
    }
}
