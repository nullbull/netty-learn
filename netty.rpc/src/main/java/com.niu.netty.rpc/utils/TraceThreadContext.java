package com.niu.netty.rpc.utils;

import com.niu.netty.rpc.protol.NiuTrace;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/8/13 22:53
 * @desc
 */
public class TraceThreadContext {
    private static ThreadLocal<NiuTrace> localTraceContext = new ThreadLocal<>();
    public static void set(NiuTrace trace) {
        localTraceContext.set(trace);
    }
    public static NiuTrace get() {
        return localTraceContext.get();
    }
    public static void remove() {
        localTraceContext.remove();
    }
}
