package org.lxz.tools.syncdevice.event

import org.lxz.tools.syncdevice.bean.DataBean
import org.lxz.tools.syncdevice.client.DeviceConnect
import com.google.gson.Gson
import org.lxz.tools.syncdevice.client.OutPutSteamMode
import java.util.regex.Pattern
import org.lxz.tools.syncdevice.client.TcpService




/**
 * Created by linxingzhu on 2018/7/10.
 */
class  IsLiveEventTask : EventTask() {


    override fun receiveProcess(message: String, hex: String, instruct: List<String>): Boolean {
        return instruct.size == 3
                && "\$php->".equals(instruct[0])
                && "IsLive".equals(instruct[1])
                && Pattern.compile("[0-9]*").matcher(instruct[2]).matches()
    }

    override fun timeOut(): Long = 5000



    override fun disposeEvent(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService) {
        /** 将数据提交到队列  */
        println("开始执行新开锁操作")
        var deviceId=instruct[2]
        val model = DataBean("success",null,null,null)
        val list = TcpService.instaince.connectDeviceMap
        try {
            model.result = list.contains(deviceId)
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            model.result = false
        }
        TcpService.instaince.write(client,Gson().toJson(model), OutPutSteamMode.DEFAULT)
        setFinish()
    }

    override fun deviceAccept(receive: String, hex: String, client: DeviceConnect, service: TcpService) {

    }

}