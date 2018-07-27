package org.lxz.tools.syncdevice.client

import org.lxz.tools.syncdevice.event.*
import org.lxz.tools.syncdevice.helper.HexHelper
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by linxingzhu on 2018/6/28.
 */
interface DeviceConnectListListen {
    fun list(connectMap:HashMap<String, DeviceConnect>)
    fun addDevice(deviceConn: DeviceConnect)
    fun remove(deviceConn: DeviceConnect)
}

interface LogListen{
    /**输入与触发事件监听*/
    fun received(received:String, hex:String, device: DeviceConnect, event:EventTask, code:Int)
    /**输出监听*/
    fun write(connect: DeviceConnect, send:String, mode: OutPutSteamMode)
}


interface ServiceEventListen{
    fun event(code:Int)
}
enum class OutPutSteamMode(val desc: String) {
    DEFAULT("字符串形式"),
    HEX("16进制形式");


}
class TcpService private constructor():Runnable {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            instaince.startServer(LocadEventClass)
        }

        val instaince: TcpService by lazy{ TcpService() }


        const val ERROR_STATUS="error"
        const val SUCCESS_STATUS="success"
        const val ERROR_CODE_INTERRRUPTED=1001
        const val ERROR_CODE_TIME_OUT=1002
        const val ERROR_CODE_CANNET_NOT_CALLBACK_DATA=1003
        const val ERROR_CODE_CANNET_NOT_FIND_DEVICE=1004
        /**处理*/
        const val STATUS_CODE_DISPOSE=10
        const val STATUS_CODE_ACCEPT=11
        const val STATUS_CODE_ACCEPT_BUT_NOT_CALLBACK_DATA=12
        const val STATUS_CODE_DISPOSE_BUT_NOT_CALLBACK_DATA=13

        const val EVENT_INIT_CODE=9001;
        const val EVENT_CLOSE_CODE=9002
    }
    var deviceConnectThreaadPool: ExecutorService?=null
    val version = "1.2"
    val port = 9501
    /**异步读写锁*/
    val synclock = ReentrantLock()

    var eventChainTask = arrayListOf<Class<out EventAction>>()




    /**TCP服务设备连接列表*/
    val connectDeviceMap = HashMap<String, DeviceConnect>()
    /**TCP服务非设备列表*/
    val connecNonDeviceList= arrayListOf<DeviceConnect>()

    init{println("Build TcpService Version:" + version +" port:"+port) }

    private var mDeviceConnectListListen: DeviceConnectListListen?=null
    fun setDeviceConnectListListen(deviceConn: DeviceConnectListListen?): TcpService {
        mDeviceConnectListListen=deviceConn
        return this
    }
    fun getDeviceConnectListListen(): DeviceConnectListListen?=mDeviceConnectListListen
    private var mLogListen: LogListen?=null
    fun setLogListen(log: LogListen): TcpService {
        mLogListen=log
        return this
    }
    fun getLogListen(): LogListen?=mLogListen

    private var mServiceEventListen: ServiceEventListen?=null
    fun setServiceEventListen(event: ServiceEventListen): TcpService {
        mServiceEventListen=event
        return this
    }
    fun getServiceEventListen(): ServiceEventListen?=mServiceEventListen


    /**创建一条本地指令*/
    fun command(content:String,mode: OutPutSteamMode,listen:DeviceConnect.ReadDataListen){
        var conn=DeviceConnect(Socket("127.0.0.1",port))
        conn.setReadDataListen(listen)
        write(conn,content,mode)

    }



    fun syncExecute(action:()->Unit){
        try {
            synclock.lock()
            action()
            synclock.unlock()
        } catch (e: Exception) {
        }

    }
    fun write(connect: DeviceConnect, content:String, mode: OutPutSteamMode)
    {
        syncExecute {
            when(mode)
            {
                OutPutSteamMode.DEFAULT ->{
                    connect.printWriter.write(content)
                    connect.printWriter.flush()
                }
                OutPutSteamMode.HEX ->{
                    var bytes=HexHelper.hexStr2Bytes(content)
                    connect.outStream.write(bytes)
                    connect.outStream.flush()
                }
            }
            mLogListen?.write(connect,content,mode);
        }

    }





    fun startServer(clazzs:List<Class<out EventAction>>) {
        eventChainTask.clear()
        eventChainTask.addAll(clazzs)
        println("TCP端口监听启动")
        serverSocket?.close()
        if(deviceConnectThreaadPool==null||deviceConnectThreaadPool!!.isShutdown)
        {
            deviceConnectThreaadPool = Executors.newWorkStealingPool()
        }
        deviceConnectThreaadPool!!.submit(this)

    }

    var serverSocket: ServerSocket?=null

    override fun run() {
        try {
            var socket: Socket? = null
            serverSocket = ServerSocket(port)// 1024-65535的某个端口
            // 调用accept()方法开始监听，等待客户端的连接
            fun getSocket(): Socket? {
                    socket = serverSocket!!.accept()

                return socket;
            }
            mServiceEventListen?.event(EVENT_INIT_CODE)
            while (serverSocket!=null&&!(serverSocket!!.isClosed)&&getSocket() != null) {
                println("服务器监听到新连接:" + socket!!.inetAddress)
                var deviceConnect:DeviceConnect=DeviceConnect(socket!!)
                addNonDeviceList(deviceConnect)
                deviceConnectThreaadPool?.submit(deviceConnect)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("服务器停止监听")
        mServiceEventListen?.event(EVENT_CLOSE_CODE)
    }

    private fun addNonDeviceList(deviceConnect: DeviceConnect) {
        syncExecute {
            connecNonDeviceList.add(deviceConnect)
        }
    }

    fun removeNonDeviceSocketConnectList(deviceConnect: DeviceConnect) {
        syncExecute {
            connecNonDeviceList.remove(deviceConnect)
        }
    }


    fun saveSocketConnectMap(deviceId: String, tcpSocketConnect: DeviceConnect) {
        syncExecute{
            print("添加新设备${deviceId}")
            instaince.connectDeviceMap.put(deviceId, tcpSocketConnect)
            mDeviceConnectListListen?.list(connectDeviceMap)
            connecNonDeviceList.remove(tcpSocketConnect)
            mDeviceConnectListListen?.addDevice(tcpSocketConnect)
        }
    }
    fun removeSocketConnectMap(deviceId: String, tcpSocketConnect: DeviceConnect) {
        syncExecute {
            print("移除设备${deviceId}")
            instaince.connectDeviceMap.remove(deviceId)
            mDeviceConnectListListen?.list(connectDeviceMap)
            mDeviceConnectListListen?.remove(tcpSocketConnect)
        }
    }

    fun stopServect() {
        //关闭所有连接
        closeAllConnect()
        deviceConnectThreaadPool?.shutdown()
        serverSocket?.close()

    }

    private fun closeAllConnect() {
        closeDeviceConnect()
        closeNonDeviceConnect()
    }

    private fun closeNonDeviceConnect() {

      syncExecute {
          var it=connecNonDeviceList.iterator()
          while (it.hasNext())
          {
              it.next().outStream.close()
          }
          connecNonDeviceList.clear()
      }
    }

    private fun closeDeviceConnect() {
        var it=connectDeviceMap.iterator()
        while (it.hasNext())
        {
            it.next().value.outStream.close()
        }
        connectDeviceMap.clear()
    }


}
