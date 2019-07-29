package com.niu.netty.rpc.server;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/29 23:06
 * @desc
 */
public class NiuDefaultThreadFactory implements ThreadFactory {
    private final static AtomicInteger poolNumer = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final String namePrefix;

    public NiuDefaultThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();

        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix + "-" + poolNumer.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        thread.setDaemon(true);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
