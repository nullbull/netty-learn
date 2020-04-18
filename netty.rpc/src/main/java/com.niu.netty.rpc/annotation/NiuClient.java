package com.niu.netty.rpc.annotation;

import java.lang.annotation.*;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 14:43
 * @desc:
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface NiuClient {
    String zkPath() default "";

    String serverIpPorts() default "";

    String genericService() default "";

    boolean cat() default false;

    int connectionTimeout() default N
}
