package com.niu.netty.rpc.utils;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/30 22:31
 * @desc
 */
//@RunWith(JUnit4.class)
public class NiuExceptionUtilTest {

    @Test
    public void getExceptionInfo() {
        RuntimeException exception = new RuntimeException("zwtzz");
        System.out.println(NiuExceptionUtil.getExceptionInfo(exception));
    }

    @Test
    public void testGetExceptionInfo() {
    }
}
