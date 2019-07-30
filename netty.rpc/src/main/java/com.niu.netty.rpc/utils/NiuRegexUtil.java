package com.niu.netty.rpc.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/30 22:43
 * @desc
 */
public class NiuRegexUtil {
    private final static ConcurrentHashMap<String, Pattern> PatternMap = new ConcurrentHashMap<>(8);
    public static boolean match(String regex, String str) {
        Pattern pattern = PatternMap.computeIfAbsent(regex, Pattern::compile);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public static void main(String[] args) {
        String regex = "([1-9])";
        System.out.println(NiuRegexUtil.match(regex, "a"));
        String regex2 = "[a-zA-Z]";
        System.out.println(NiuRegexUtil.match(regex2, "aAA"));
        System.out.println(NiuRegexUtil.match(regex, "5"));
    }
}
