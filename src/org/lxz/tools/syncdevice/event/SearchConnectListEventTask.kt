package org.lxz.tools.syncdevice.event

import org.lxz.tools.syncdevice.client.DeviceConnect
import com.google.gson.Gson
import org.lxz.tools.syncdevice.client.OutPutSteamMode
import org.lxz.tools.syncdevice.client.TcpService




/**
 * Created by linxingzhu on 2018/7/10.
 */
class  SearchConnectListEventTask : EventTask() {


    override fun receiveProcess(message: String, hex: String, instruct: List<String>): Boolean {
        return instruct.size == 2
                && "\$php->".equals(instruct[0])
                && "SearchList".equals(instruct[1])
    }

    override fun timeOut(): Long = 5000



    override fun disposeEvent(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService) {
        /** 将数据提交到队列  */
        println("查询当前已连接的设备列表")
        val list = TcpService.instaince.connectDeviceMap.keys
        TcpService.instaince.write(client,Gson().toJson(list), OutPutSteamMode.DEFAULT)
        setFinish()

    }



    override fun deviceAccept(receive: String, hex: String, client: DeviceConnect, service: TcpService) {

    }

}