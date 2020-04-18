package com.niu.netty.rpc.client.async;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.async.AsyncMethodCallback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * @author: niuzhenhao
 * @date: 2019-08-31 15:10
 * @desc:
 */
@Slf4j
public class NiuAsyncCallBack<R, T> implements AsyncMethodCallback<T> {
    private R r;
    private Throwable e;
    private final CountDownLatch finished = new CountDownLatch(1);
    private Future<R> future = new Future<R>() {
        private volatile boolean cancelled = false;
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (finished.getCount() > 0) {
                cancelled = true;
                finished.countDown();
                return true;
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return finished.getCount() == 0;
        }

        @Override
        public R get() throws InterruptedException, ExecutionException {
            finished.await();
            return getValue();
        }

        @Override
        public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (finished.await(timeout, unit)) {
                return getValue();
            } else {
                throw new TimeoutException("NiuAsync TimeoutException");
            }
        }
        private R getValue() throws ExecutionException, CancellationException {
            if (null != e) {
                throw new ExecutionException("Observer on Failure", e);
            } else if (cancelled) {
                throw new CancellationException("Subscriber unsubscribed");
            } else {
                return r;
            }
        }
    };

    public Future<R> getFuture() {
        return future;
    }

    @Override
    public void onComplete(T t) {
        Method method;
        Object o;
        try {
            method = t.getClass().getDeclaredMethod("getResult");
            o = method.invoke(t);
            r = (R) o;
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause () instanceof TApplicationException
                    && ((TApplicationException) e.getCause ()).getType () == TApplicationException.MISSING_RESULT) {
                r = null;
            } else {
                onError (e);
                return;
            }
        }
        finished.countDown();
    }

    @Override
    public void onError(Exception e) {
        log.error("the niu Async faild", e);
        this.e = e;
        finished.countDown();
    }
    public void onCompleteWithoutReflect(Object o) {
        r = (R) o;
        finished.countDown();
    }
}
