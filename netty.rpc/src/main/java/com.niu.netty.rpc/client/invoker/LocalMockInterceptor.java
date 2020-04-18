package com.niu.netty.rpc.client.invoker;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * @author: niuzhenhao
 * @date: 2019-09-25 20:21
 * @desc:
 */
public class LocalMockInterceptor implements MethodInterceptor {

    private String serviceImpl;

    public LocalMockInterceptor(String serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        Object[] arguements = methodInvocation.getArguments();
        Object target = Class.forName(serviceImpl).newInstance();
        Object result = method.invoke(target, arguements);
        return result;
    }
}
