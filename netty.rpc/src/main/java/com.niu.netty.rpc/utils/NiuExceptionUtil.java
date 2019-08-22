package com.niu.netty.rpc.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/30 22:23
 * @desc
 */
public class NiuExceptionUtil {
    public static String getExceptionInfo(Exception e) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(out)) {
            e.printStackTrace(printStream);
            String ret = new String(out.toByteArray());
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
