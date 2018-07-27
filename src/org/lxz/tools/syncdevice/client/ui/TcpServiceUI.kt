package org.lxz.tools.syncdevice.client.ui

import org.lxz.tools.syncdevice.client.*
import com.google.gson.Gson
import org.lxz.tools.syncdevice.event.*
import java.awt.EventQueue
import java.util.*

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.border.EmptyBorder


class TcpServiceUI : JFrame() {
    private val contentPane: JPanel
    private val et_ip: JTextField
    private val et_port: JTextField
    private val btn_connect: JButton
    private var et_log: JTextPane
    private var et_list: JTextPane


    private var isStartService=false

    fun onClick()
    {

        if(isStartService)
        {
            TcpService.instaince.stopServect()
            return
        }
        var port=0
        try {
            port = Integer.valueOf(et_port.getText())
            if (port < 0||port>65535) {
                showErrorMessage("输入的端口必须大于0")
                return
            }
        } catch (e: NumberFormatException) {
            return
        }
        et_list.text= Gson().toJson(arrayListOf<String>())
        TcpService.instaince.setDeviceConnectListListen(object: DeviceConnectListListen {
            override fun list(connectMap: HashMap<String, DeviceConnect>) {
               et_list.text= Gson().toJson(connectMap.keys)


            }

            override fun addDevice(deviceConn: DeviceConnect) {
                log("当前已添加一个新设备:"+deviceConn.deviceId)

            }

            override fun remove(deviceConn: DeviceConnect) {
                log("设备断开连接:"+deviceConn.deviceId)
            }

        }).setLogListen(object: LogListen {
            override fun received(received: String, hex: String, device: DeviceConnect, event: EventTask, code: Int) {
                var message:String=when(code)
                {
                  TcpService.STATUS_CODE_ACCEPT ->"收到反馈消息"
                  TcpService.STATUS_CODE_ACCEPT_BUT_NOT_CALLBACK_DATA ->"收到反馈消息-但超时了"
                  TcpService.STATUS_CODE_DISPOSE ->"处理消息"
                  TcpService.STATUS_CODE_DISPOSE ->"处理消息-但超时了"
                  else->""
                }
                log("当前处理:${getDeviceName(device)} 事件 -${event.javaClass.simpleName}  接收信息 - ${received} 16进制转化 ${hex}" )

            }

            override fun write(connect: DeviceConnect, send: String, mode: OutPutSteamMode) {
                log("当前连接设备:${getDeviceName(connect)} 发送 - ${send} ${mode.desc}")

            }

            fun getDeviceName(device: DeviceConnect):String{
                return "${if(device.deviceId==null)device.socket.inetAddress else device.deviceId}"
            }


        }).setServiceEventListen(object: ServiceEventListen {
            override fun event(code: Int) {
                when(code)
                {
                    TcpService.EVENT_INIT_CODE ->{
                        isStartService=true
                        btn_connect.text="关闭服务"
                    }
                    TcpService.EVENT_CLOSE_CODE -> {
                        btn_connect.text="开启服务"
                        isStartService = false
                        et_list.text=""
                        messageList.clear()
                        et_log.text=""
                    }
                }
            }

        })
        .startServer(LocadEventClass)

    }

    /**
     * Create the frame.
     */
    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setBounds(100, 100, 543, 540)
        title = "脚本服务器"
        contentPane = JPanel()
        contentPane.border = EmptyBorder(5, 5, 5, 5)
        setContentPane(contentPane)
        contentPane.layout = null

        et_ip = JTextField()
        et_ip.isEnabled = false
        et_ip.text = "127.0.0.1"
        et_ip.setBounds(50, 35, 171, 21)
        contentPane.add(et_ip)
        et_ip.columns = 10

        val lblIp = JLabel("IP:")
        lblIp.setBounds(30, 35, 60, 16)
        contentPane.add(lblIp)

        val lblPort = JLabel("port:")
        lblPort.setBounds(256, 35, 44, 16)
        contentPane.add(lblPort)

        et_port = JTextField()
        et_port.columns = 10
        et_port.setBounds(297, 35, 110, 21)
        contentPane.add(et_port)

        btn_connect = JButton("开启服务")
        btn_connect.addActionListener { }
        btn_connect.setBounds(419, 24, 96, 40)
        contentPane.add(btn_connect)
        btn_connect.addActionListener {
            onClick()
        }


        val scrollPane_1 = JScrollPane()
        scrollPane_1.setBounds(30, 276, 485, 220)
        contentPane.add(scrollPane_1)
        et_log = JTextPane()
        scrollPane_1.setViewportView(et_log)

        val lblNewLabel_1 = JLabel("当前连接的设备:")
        lblNewLabel_1.setBounds(30, 76, 123, 16)
        contentPane.add(lblNewLabel_1)


        val scrollPane_2 = JScrollPane()
        scrollPane_2.setBounds(30, 104, 481, 151)

        et_list = JTextPane()
        contentPane.add(scrollPane_2)
        scrollPane_2.setViewportView(et_list)


    }

    fun showErrorMessage(msg: String) {
        JOptionPane.showMessageDialog(null, msg, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE)
    }


    companion object {

        /**
         * Launch the application.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            EventQueue.invokeLater {
                try {
                    val frame = TcpServiceUI()
                    frame.setLocationRelativeTo(null)
                    frame.isVisible = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val messageList = LinkedList<String>()
    @Synchronized
    fun log(str: String) {
        messageList.add(str)
        if (messageList.size > 100) {
            messageList.pollFirst()
        }
        val sb = StringBuffer()
        for (content in messageList) {
            sb.append(content)
            sb.append("\n")
        }
        et_log.text = sb.toString()
    }

}
