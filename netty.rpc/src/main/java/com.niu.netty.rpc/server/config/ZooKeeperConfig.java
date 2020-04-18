package com.niu.netty.rpc.server.config;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: niuzhenhao
 * @date: 2019-08-18 17:38
 * @desc:
 */
@Data
@AllArgsConstructor
public class ZooKeeperConfig {
    private String zkPath;
    private String service;
    private String env;
    private int port;
    private int weight;
    private String server;
}
