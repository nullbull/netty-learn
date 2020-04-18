package com.niu.netty.rpc.utils;

import com.niu.netty.rpc.server.NiuDefaultThreadFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/29 23:03
 * @desc
 */
public class NiuThreadSelectorWorkerExecutorUtil {
    public static ThreadPoolExecutor getWorkerExecutor(int threadCount, NiuDefaultThreadFactory niuDefaultThreadFactory) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadCount, threadCount, 30L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), niuDefaultThreadFactory);
        executor.prestartAllCoreThreads();
        return executor;
    }
    public static ThreadPoolExecutor getWorkExecutorWithQueue(int min, int max, int workQueueSize, NiuDefaultThreadFactory threadFactory) {
        ThreadPoolExecutor executor;
        if (workQueueSize <= 0) {
            executor = new ThreadPoolExecutor(min, max, 30L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), threadFactory);
        } else {
            executor = new ThreadPoolExecutor(min, max, 30L, TimeUnit.SECONDS,
                    new LinkedBlockingDeque<>(workQueueSize), threadFactory);
        }
        executor.prestartAllCoreThreads();
        return executor;
    }
}
