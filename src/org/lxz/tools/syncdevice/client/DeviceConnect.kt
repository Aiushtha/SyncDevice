package org.lxz.tools.syncdevice.client

import org.lxz.tools.syncdevice.event.*
import org.lxz.tools.syncdevice.helper.HexHelper
import org.lxz.tools.syncdevice.helper.TimeHelper
import java.io.*
import java.net.Socket

/**
 * Created by linxingzhu on 2018/7/10.
 */
class DeviceConnect(val socket: Socket) : Runnable {
    var outStream: OutputStream
    var inStream: InputStream
    var reader: BufferedReader
    var deviceId: String? = null
    val createTime: String
    var printWriter: PrintWriter
    var events:ArrayList<EventAction> = arrayListOf()
    interface ReadDataListen{
        fun read(connect: DeviceConnect,message:String,hex:String,byte:ByteArray,length:Int)
    }
    private var mReadDataListen:ReadDataListen?=null

    fun setReadDataListen(listen:ReadDataListen):DeviceConnect
    {   mReadDataListen=listen
        return this }

    fun getReadDataListen():ReadDataListen?=mReadDataListen


    init {
        createTime = TimeHelper.getNowTime()
        outStream = socket.getOutputStream()
        printWriter = PrintWriter(outStream)
        inStream = socket.getInputStream()
        val streamReader = InputStreamReader(inStream)
        reader = BufferedReader(streamReader)


    }

    var openEnabledReceiverTask:ArrayList<EventAction> = arrayListOf();

    override fun run() {
        val buffer = ByteArray(1024 * 4)
        var n = 0
        fun read(): Int {
            n = inStream.read(buffer)
            return n;
        }
        while ((read()) != -1) {
            val message = String(buffer, 0, n)
            val hex = HexHelper.byte2HexStr(buffer, n)
            mReadDataListen?.read(this,message,hex,buffer,n);
            println("message:->" + message);
            println("hex:->" + hex);

            TcpService.instaince.syncExecute {
                var it=openEnabledReceiverTask.iterator();
                while(it.hasNext())
                {
                    println("当前有一条任务等待回调")
                    it.next().deviceAcceptTaskInQueue(message, hex, this, TcpService.instaince)
                }

            }
            events.clear()
            for(eventClass in TcpService.instaince.eventChainTask)
            {
                events.add(eventClass.newInstance())
            }
          for(task in  events) {
              var instruct=task.toCommand(message)

              try {
                  var isProcessing = task.receiveProcess(message, hex, instruct)
                  if (isProcessing) {
                      task.disposeEventTaskInQueue(message, hex, instruct, this, TcpService.instaince)
  //                    events.remove(task)
  //                    events.add(task.javaClass.newInstance())
                      break
                  }
              } catch (e: Exception) {
              }
          }
        }

        when(deviceId!=null){
            true->TcpService.instaince.removeSocketConnectMap(deviceId!!, this)
            else->TcpService.instaince.removeNonDeviceSocketConnectList(this)
        }
        println("IP:" + socket.inetAddress + " 命令 " + deviceId + " 断开连接")

    }
}
