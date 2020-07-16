package com.niu.netty.rpc.heartbeat.impl;

import com.niu.netty.rpc.utils.IPUtil;
import com.niu.netty.rpc.heartbeat.request.HeartBeat;
import com.niu.netty.rpc.heartbeat.service.HeartbeatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;


import java.text.SimpleDateFormat;
import java.util.Date;


@Slf4j
public class HeartbeatServiceImpl implements HeartbeatService.Iface {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");


    @Override
    public HeartBeat getHeartBeat(HeartBeat heartBeat) throws TException {
        log.info( "HeartBeat info :{}" ,heartBeat );
        HeartBeat heartBeatRespone = new HeartBeat();
        heartBeatRespone.setIp ( IPUtil.getIPV4 () );
        heartBeatRespone.setServiceName (  heartBeat.getServiceName ());
        heartBeatRespone.setDate ( sdf.format ( new Date (  ) ) );
        return heartBeatRespone;
    }
}
