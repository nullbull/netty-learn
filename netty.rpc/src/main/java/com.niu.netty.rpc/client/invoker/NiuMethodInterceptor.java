package com.niu.netty.rpc.client.invoker;


import com.niu.netty.rpc.client.NiuClientProxy;
import com.niu.netty.rpc.client.cluster.Icluster;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * @author: niuzhenhao
 * @date: 2019-08-31 17:15
 * @desc:
 */

@Slf4j
@AllArgsConstructor
public class NiuMethodInterceptor implements MethodInterceptor {

    private Icluster icluster;
    private int retryTimes;
    private boolean retryRequest;
    private NiuClientProxy niuClientProxy;
    private int asyncTimeOut;
    private boolean cat;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        String methodName = method.getName();
        Object[] args = methodInvocation.getArguments();

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (Object.class.equals(method.getDeclaredAnnotations())) {
            try {
                return method.invoke(this, args);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage() + " className:" + niuClientProxy.);
            }
        }
    }
}
