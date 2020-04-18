package com.niu.netty.rpc.client.async;

import com.niu.netty.rpc.client.cluster.ServerObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author: niuzhenhao
 * @date: 2019-08-31 15:29
 * @desc:
 */
@Slf4j
@AllArgsConstructor
public class ReleaseResourcesNiuAsyncCallBack<T> implements AsyncMethodCallback<T> {
    private AsyncMethodCallback<T> asyncMethodCallback;
    private ServerObject serverObject;
    private TTransport socket;

    @Override
    public void onComplete(T t) {
        Method method;
        Object o;
        try {
            serverObject.getGenericObjectPool().returnObject(socket);
            method = t.getClass().getDeclaredMethod("getResult");
            o = method.invoke(t);
            if (null != asyncMethodCallback) {
                if (asyncMethodCallback instanceof  NiuAsyncCallBack) {
                    ((NiuAsyncCallBack) asyncMethodCallback).onCompleteWithoutReflect(o);
                } else {
                    asyncMethodCallback.onComplete(t);
                }
            }
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause() instanceof TApplicationException &&((TApplicationException) e.getCause()).getType() == TApplicationException.MISSING_RESULT) {
                if (null != asyncMethodCallback) {
                    if (asyncMethodCallback instanceof NiuAsyncCallBack) {
                        ((NiuAsyncCallBack) asyncMethodCallback).onCompleteWithoutReflect(null);
                    } else {
                        asyncMethodCallback.onComplete(t);
                    }
                }
            }
        }
    }

    @Override
    public void onError(Exception e) {
        try {
            serverObject.getGenericObjectPool().invalidateObject(socket);
        } catch (Exception e1) {
            log.error("onError invalidateObject object error", e);
        }
        asyncMethodCallback.onError(e);
    }
}
