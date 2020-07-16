package com.niu.netty.rpc.annotation;

import java.lang.annotation.*;

/**
 * @author niuzhenhao
 * @date 2020/7/16 16:25
 * @desc
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NiuServer {

    int port();

    String zkpath() default "";

    boolean cat() default false;

    int bossThreadCount() default 0;

    int workThreadCount() default 0;

    int koalasThreadCount() default 0;

    int maxLength() default Integer.MAX_VALUE;

    String env() default "dev";

    int weight() default 10;

    String serverType() default "NETTY";

    int workQueue() default 0;

    String privateKey() default "";

    String publicKey() default "";

}
