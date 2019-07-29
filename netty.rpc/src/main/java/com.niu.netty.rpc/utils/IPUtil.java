package com.niu.netty.rpc.utils;

import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/29 22:35
 * @desc
 */
public class IPUtil {
    public static String getIPV4() {
        String ip = "";
        Enumeration<NetworkInterface> networkInterface;
        try {
            networkInterface = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return ip;
        }
        Set<String> ips = new HashSet<>();
        while (networkInterface.hasMoreElements()) {
            NetworkInterface ni = networkInterface.nextElement();
            Enumeration<InetAddress> inetAddress = null;
            try {
                if (null != ni) {
                    inetAddress = ni.getInetAddresses();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (null != inetAddress && !inetAddress.hasMoreElements()) {
                InetAddress ia = inetAddress.nextElement();
                if (ia instanceof Inet6Address) {
                    continue;
                }
                String thisIP = ia.getHostAddress();
                if (!ia.isLoopbackAddress() && !thisIP.contains(":") && !"127.0.0.1".equals(thisIP)) {
                    ips.add(thisIP);
                    if (StringUtils.isEmpty(ip)) {
                        ip = thisIP;
                    }
                }
            }
        }
        if (StringUtils.isEmpty(ip)) {
            ip =  "";
        }
        return ip;
    }
    public static String getClientIP(ChannelHandlerContext ctx) {
        String ip;
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            ip = socketAddress.getAddress().getHostAddress();
        } catch (Exception e) {
            ip = "UNKNOWN";
        }
        return ip;
    }
}
