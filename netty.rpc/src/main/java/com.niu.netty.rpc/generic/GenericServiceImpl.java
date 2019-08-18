package com.niu.netty.rpc.generic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 16:12
 * @desc:
 */
@Slf4j
public class GenericServiceImpl implements GenericService.Iface{

    private Object realImpl;
    private String realClassName;

    public GenericServiceImpl(Object realImpl) {
        this.realImpl = realImpl;
    }

    @Override
    public String invoke(GenericRequest request) throws TException {
        String methodName = request.getMethodName();
        List<String> clazzType = request.getClassType();
        List<String> requestObject = request.getRequestObj();
        Class<?> targetImpl = AopUtils.getTargetClass(realImpl);
        this.realClassName = targetImpl.getName();


        List<Class> realClassTypes = null;
        if (null != clazzType && !clazzType.isEmpty()) {
            realClassTypes = new ArrayList<>();
            for (String clazz : clazzType) {
                try {
                    realClassTypes.add(getClassType(clazz));
                } catch (ClassNotFoundException e) {
                    throw new TException("class:" + realClassName + ",classType:" + clazz + " not found in the server side!");
                }
            }
        }
        Method method = null;
        List<Object> realRequest = null;
        try {
            if (null == realClassTypes || realClassTypes.isEmpty()) {
                method = targetImpl.getMethod(methodName);
            } else{
                method = targetImpl.getMethod(methodName, realClassTypes.toArray(new Class[0]));
            }
         }catch (NoSuchMethodException e) {
            throw new TException("class:" + realClassName + ",method:" + methodName + "not found !");
         }
        realRequest = new ArrayList<>();
        for (int i = 0; i < requestObject.size(); i++) {
            try {
                Object o = JSONObject.parseObject(requestObject.get(i), realClassTypes.get(i));
                realRequest.add(o);
            } catch (Exception e) {
                throw new TException("class:" + realClassName + ",method:" + methodName + ",JSONObject.parsetObject error, text:" + requestObject.get(i));
            }
        }
        Object object = null;
        try {
            if (null == realClassTypes || realClassTypes.isEmpty()) {
                object = method.invoke(realImpl);
            } else {
                object = method.invoke(realImpl, realRequest.toArray(new Object[0]));
            }
        } catch (Exception e) {
            Throwable cause = null != e.getCause() ? e.getCause() : e;
            throw new TException(cause);
        }
        if (null == object) {
            return "VOID";
        } else {
            return JSON.toJSONString(object);
        }
    }

    private Class<?> getClassType(String stringType) throws ClassNotFoundException {
        switch (stringType) {
            case "int" :
                return int.class;
            case "double":
                return double.class;
            case "long":
                return long.class;
            case "short":
                return short.class;
            case "byte":
                return byte.class;
        }
    }

}
