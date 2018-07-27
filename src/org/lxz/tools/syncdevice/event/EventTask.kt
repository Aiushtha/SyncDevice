package org.lxz.tools.syncdevice.event

import org.lxz.tools.syncdevice.bean.DataBean
import com.google.gson.Gson
import org.lxz.tools.syncdevice.client.DeviceConnect
import org.lxz.tools.syncdevice.client.OutPutSteamMode
import org.lxz.tools.syncdevice.client.TcpService
import org.lxz.tools.syncdevice.client.TcpService.Companion.ERROR_CODE_INTERRRUPTED
import org.lxz.tools.syncdevice.client.TcpService.Companion.ERROR_CODE_TIME_OUT
import org.lxz.tools.syncdevice.client.TcpService.Companion.ERROR_STATUS
import org.lxz.tools.syncdevice.client.TcpService.Companion.STATUS_CODE_DISPOSE
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern

/**
 * Created by linxingzhu on 2018/7/10.
 */


abstract class EventTask : EventAction {

    private lateinit var fristClient: DeviceConnect;
    private lateinit var acceptClient: DeviceConnect;

    var isFinished=false;

    private lateinit var acitons: LinkedList<Runnable>

    override fun toCommand(str: String): List<String> =  toCommand(str,regx())
    override fun regx():String = " "

    fun toCommand(str: String, regx: String): List<String> = when(true)
        {
            regx==null||" ".equals(regx) -> str.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().toList()
            else -> {
                var list= arrayListOf<String>()
                val pattern = Pattern.compile(regx)
                val matcher = pattern.matcher(str)
                while (matcher.find()) {
                    list.add(matcher.group().trim())
                }
                list
            }

        }

    @Synchronized
    override fun isFinish():Boolean{
        return isFinished
    }

    @Synchronized
    override fun setFinish(){
        try {
            syncTaskLock.lock()
            closeAllDeviceAccept()
            isFinished=true
            syncTaskCondition.signalAll()
            syncTaskLock.unlock()
        } catch (e: Exception) {
        }


    }


    override fun waitTaskUntilFinish(){
        if(!isFinish()) {
            syncTaskLock.lock()
            syncTaskCondition.await()
            syncTaskLock.unlock()
        }
    }



    val syncTaskLock = ReentrantLock()
    val syncTaskCondition = syncTaskLock.newCondition()

    companion object {
        var exeService: ExecutorService = Executors.newCachedThreadPool()
        var timeOutService: ExecutorService = Executors.newCachedThreadPool()

    }

    private var connects= arrayListOf<DeviceConnect>()
    /**是否有设备已经开启消息监听*/
    override fun isDeviceAccept(conn: DeviceConnect):Boolean{
       var isAccept:Boolean=false;
       TcpService.instaince.syncExecute {
           println("is当前监听器数量："+conn.openEnabledReceiverTask.size)
           isAccept= !(conn.openEnabledReceiverTask.isEmpty())
       }
        return isAccept;
    }

    override fun openDeviceAccept(conn: DeviceConnect) {
        TcpService.instaince.syncExecute {
            connects.add(conn)
            conn.openEnabledReceiverTask.add(this)
        }
    }

    fun closeAllDeviceAccept() {
        TcpService.instaince.syncExecute {
            for(c in connects) {
                c.openEnabledReceiverTask.clear()
                println("is当前监听器数量："+c.openEnabledReceiverTask.size)
            }
            connects.clear()
            println("清空所有监听器")

        }
    }

    override fun closeDeviceAccept(conn: DeviceConnect) {
        TcpService.instaince.syncExecute {
            conn.openEnabledReceiverTask.clear()
        }
    }

    override fun getFristConnectClient(): DeviceConnect {
        return fristClient
    }
    /**返回待监听的客户端*/
    override fun getAcceptConnectClient(): DeviceConnect?=acceptClient

    abstract override fun receiveProcess(message: String, hex: String, instruct: List<String>): Boolean


    abstract override fun timeOut(): Long


    abstract override fun disposeEvent(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService)


    abstract override fun deviceAccept(receive: String, hex: String, client: DeviceConnect, service: TcpService)

    override fun addTask(run: Runnable) {
        syncTaskLock.lock()
        acitons.add(run)
        syncTaskCondition.signalAll()
        syncTaskLock.unlock()
    }



    override fun disposeEventTaskInQueue(message: String, hex: String, instruct: List<String>, client: DeviceConnect, service: TcpService) {
        fristClient=client;
        acitons=LinkedList()
        addTask(Runnable { disposeEvent(message, hex, instruct, client, service)})
        TcpService.instaince.getLogListen()?.received(message,hex,client,this,STATUS_CODE_DISPOSE)
         timeOutService.submit({
            var future = exeService.submit(object : Callable<Boolean> {
                override fun call(): Boolean {
                    while (!isFinish())
                    {
                      while (!acitons.isEmpty())
                      {
                          acitons.pollFirst().run()
                      }

                      waitTaskUntilFinish()

                    }
                    println("执行结束")
                    return true
                }

            })
            timeOut(message,hex,future,client,false);
        })

    }


    private fun timeOut(message: String, hex: String, future: Future<Boolean>, client: DeviceConnect, isReceive: Boolean) {
        var data=DataBean("","","",null);

        try {
            future.get(timeOut(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace() //get为一个等待过程，异常中止get会抛出异常
            data.result=ERROR_STATUS
            data.code=ERROR_CODE_INTERRRUPTED;
        } catch (e: ExecutionException) {
            e.printStackTrace() //submit计算出现异常
            data.result=ERROR_STATUS
            data.code=ERROR_CODE_INTERRRUPTED;
        } catch (e: TimeoutException) {
            e.printStackTrace() //超时异常
            data.result=ERROR_STATUS
            data.code= ERROR_CODE_TIME_OUT;
        }
        if(data.code!=null)
        {
            TcpService.instaince.write(getFristConnectClient(),Gson().toJson(data), OutPutSteamMode.DEFAULT)
            when(isReceive)
            {
                true-> TcpService.instaince.getLogListen()?.received(message,hex,client,this, TcpService.STATUS_CODE_ACCEPT_BUT_NOT_CALLBACK_DATA)
                else-> TcpService.instaince.getLogListen()?.received(message,hex,client,this, TcpService.STATUS_CODE_DISPOSE_BUT_NOT_CALLBACK_DATA)
            }

        }
        closeAllDeviceAccept()

    }

    override fun deviceAcceptTaskInQueue(receive: String, hex: String, client: DeviceConnect, service: TcpService) {
        acceptClient=client
        TcpService.instaince.getLogListen()?.received(receive,hex,client,this,STATUS_CODE_DISPOSE)
        addTask(Runnable { deviceAccept(receive, hex, client, service) })
    }

}