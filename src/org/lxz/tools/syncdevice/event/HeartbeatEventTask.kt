package org.lxz.tools.syncdevice.event

import org.lxz.tools.syncdevice.client.DeviceConnect
import org.lxz.tools.syncdevice.client.OutPutSteamMode
import org.lxz.tools.syncdevice.client.TcpService
import java.util.regex.Pattern


/**
 * Created by linxingzhu on 2018/7/10.
 */
class HeartbeatEventTask : EventTask() {

    override  fun receiveProcess(message:String,hex:String,instruct:List<String>):Boolean{
        var pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(message).matches()
    }

    override fun timeOut(): Long = 5000


    override fun disposeEvent(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService) {
        //默认收到的第一消息是设备的id 并加入到存活的服务器连接列表
        if(client.deviceId==null)
        {
            client.deviceId=message
            service.saveSocketConnectMap(message,client)
        }
        TcpService.instaince.write(client,message, OutPutSteamMode.DEFAULT)
        setFinish()

    }
    override fun deviceAccept(receive: String, hex: String, client: DeviceConnect, service: TcpService) {

    }

}