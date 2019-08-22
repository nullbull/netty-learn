package com.niu.netty.rpc.client.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

/**
 * @author: niuzhenhao
 * @date: 2019-08-22 15:48
 * @desc:
 */
@Data
@AllArgsConstructor
public class RemoteServer {

    private String ip;

    private String port;

    private int weight;

    private boolean isEnable;

    private String server;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass () != o.getClass ()) return false;
        RemoteServer that = (RemoteServer) o;
        return weight == that.weight &&
                isEnable == that.isEnable &&
                Objects.equals ( ip, that.ip ) &&
                Objects.equals ( port, that.port ) &&
                Objects.equals ( server, that.server );
    }

    @Override
    public int hashCode() {

        return Objects.hash ( ip, port, weight, isEnable, server );
    }
}
