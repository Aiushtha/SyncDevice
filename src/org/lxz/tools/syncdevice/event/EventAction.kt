package org.lxz.tools.syncdevice.event

import org.lxz.tools.syncdevice.client.DeviceConnect
import org.lxz.tools.syncdevice.client.TcpService

/**
 * Created by linxingzhu on 2018/7/10.
 */


/**一个连接事件到结束的生命周期*/
interface EventAction{
    /**
     * @param message 接收到的原始消息
     * @param hex 转化成16进制
     * @param instruct 转化以空格分割开的指令
     */
    fun receiveProcess(message:String,hex:String, instruct:List<String>):Boolean

    /**将指令转化成命令行*/
    fun toCommand(str:String):List<String>

    fun regx():String


    /**设置超时时间*/
    fun timeOut():Long

    /**返回第一个连接的客户端*/
    fun getFristConnectClient(): DeviceConnect


    /**返回待监听的客户端*/
    fun getAcceptConnectClient(): DeviceConnect?


    /**
     * @param cmd 接收的指令
     * @param client 当前发送消息的设备
     * @param connectList 已连接设备服务列表
     */
    fun disposeEvent(message:String, hex:String, instruct:List<String>, client: DeviceConnect, service: TcpService)


    /**监听指定的设备*/
    fun deviceAccept(receive:String, hex:String, client: DeviceConnect, service: TcpService)

    /**加入超时队列-处理第一个连接的设备指令*/
    fun disposeEventTaskInQueue(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService)
    /**加入超时队列-监听指定的设备*/
    fun deviceAcceptTaskInQueue(receive: String, hex: String, client: DeviceConnect, service: TcpService)


    /**对指定设备开启消息监听*/
    fun openDeviceAccept(conn: DeviceConnect)
    /**对指定设备关闭消息监听*/
    fun closeDeviceAccept(conn: DeviceConnect)

    /**是否有设备已经开启消息监听*/
    fun isDeviceAccept(conn: DeviceConnect):Boolean

    /**是否完成*/
    fun isFinish():Boolean
    /**设置完成*/
    fun setFinish()

    /**等待任务完成*/
    fun waitTaskUntilFinish()
    /**添加*/
    fun addTask(run:Runnable)

}