package com.niu.netty.rpc.client.cluster.impl;

import com.niu.netty.rpc.client.cluster.RemoteServer;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 17:12
 * @desc:
 */
public class RandomLoadBalancer extends AbstractLoadBalancer {
    @Override
    public RemoteServer doSelect(List<RemoteServer> list) {
        if (null == list) {
            return null;
        }
        //将list混排
        Collections.shuffle(list);
        int[] array = new int[list.size()];
        int total = 0;
        int current = 0;
        for (RemoteServer remoteServer : list) {
            total += getWeight(remoteServer);
            array[current++] = total;
        }
        if (total == 0) {
            throw new IllegalArgumentException("the remote serverList is EMPTY!");
        }
        Random r = new Random(47);
        current = r.nextInt(total);
        for (int i = 0; i < array.length; i++) {
            if (current < array[i]) {
                return list.get(i);
            }
        }
        return null;
    }
}
