package heartbeat.impl;

import com.niu.netty.rpc.utils.IPUtil;
import heartbeat.request.HeartBeat;
import heartbeat.service.HeartbeatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;


import java.text.SimpleDateFormat;
import java.util.Date;


@Slf4j
public class HeartbeatServiceImpl implements HeartbeatService.Iface {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");


    @Override
    public heartbeat.request.HeartBeat getHeartBeat(heartbeat.request.HeartBeat heartBeat) throws TException {
        log.info( "HeartBeat info :{}" ,heartBeat );
        HeartBeat heartBeatRespone = new HeartBeat();
        heartBeatRespone.setIp ( IPUtil.getIPV4 () );
        heartBeatRespone.setServiceName (  heartBeat.getServiceName ());
        heartBeatRespone.setDate ( sdf.format ( new Date (  ) ) );
        return heartBeatRespone;
    }
}
