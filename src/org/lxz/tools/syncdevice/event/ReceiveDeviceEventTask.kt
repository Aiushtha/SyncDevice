package org.lxz.tools.syncdevice.event

import org.lxz.tools.syncdevice.bean.StatusBean
import org.lxz.tools.syncdevice.client.DeviceConnect
import org.lxz.tools.syncdevice.client.TcpService
import com.google.gson.Gson
import org.lxz.tools.syncdevice.helper.HexHelper
import org.lxz.tools.syncdevice.helper.TimeHelper
import org.joda.time.DateTime
import org.joda.time.Seconds
import org.lxz.tools.syncdevice.client.OutPutSteamMode
import java.util.HashMap
import java.util.regex.Pattern


/**
 * Created by linxingzhu on 2018/7/10.
 */
class ReceiveDeviceEventTask : EventTask() {

    /**
     * php-> Receive 2 "22" false
     * */
    override fun receiveProcess(message: String, hex: String, instruct: List<String>): Boolean {
        return  instruct.size >= 4
                && "\$php->".equals(instruct[0])
                && "Receive".equals(instruct[1])
                && Pattern.compile("[0-9]*").matcher(instruct[2]).matches();
    }

    override fun regx():String="[\"]+.+[\"]+|[^\\s]+[ ]*+"


    override fun timeOut(): Long = 5000

    private val timeinterval = 1000L;


    private val CODE_SUSSCESS = 0
    //1表示参数错误
    private val ERROR_CODE_PARAMETER = 1
    //2表示像连接成功但写入数据失败
    private val ERROR_CODE_WRITE = 2
    //3表示锁控板未连接到服务器
    private val ERROR_CODE_NO_CONNECT = 3


    override fun disposeEvent(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService) {
        /** 将数据提交到队列*/
        val deviceId = instruct[2]
        var message = instruct[3]
        if(message.indexOf("\"")==0&&message.lastIndexOf("\"")==message.length-1)
        {
            message=message.substring(1,message.length-1)
            println(message)
        }

        var isHex=false
        if(instruct.size==4)
        {
            isHex=false;
        }else
        {
            isHex = java.lang.Boolean.valueOf(instruct[4])
        }
        var connect = service.connectDeviceMap[deviceId]
        when (true) {
            connect == null -> {
                callBackData(client, "error code:${ERROR_CODE_NO_CONNECT}")
            }
            (connect?.socket?.isConnected)==false->{
                callBackData(client, "error code:${ERROR_CODE_NO_CONNECT}")
            }
            else -> {
                //如果当前有监听等待
                while (isDeviceAccept(connect!!))
                {
                    Thread.sleep(200)
                }
                //对设备开启监听、
                openDeviceAccept(connect!!)

                try {
                    TcpService.instaince.write(connect!!,message,if(isHex) OutPutSteamMode.HEX else OutPutSteamMode.DEFAULT )

                } catch (e: Exception) {
                    callBackData(client, "error code:${ERROR_CODE_WRITE}")
                }

            }
        }
    }
    fun callBackData(client: DeviceConnect, content:String) {
        TcpService.instaince.write(client,content, OutPutSteamMode.DEFAULT)
        setFinish()
    }

    override fun deviceAccept(receive: String, hex: String, client: DeviceConnect, service: TcpService) {
        //如果为非数字
        if(!Pattern.compile("[0-9]*").matcher(receive).matches()) {
            TcpService.instaince.write(getFristConnectClient(),receive,OutPutSteamMode.DEFAULT )
            setFinish()
        }

    }
}