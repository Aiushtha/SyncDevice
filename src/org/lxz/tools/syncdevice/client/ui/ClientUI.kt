package org.lxz.tools.syncdevice.client.ui

import org.lxz.tools.syncdevice.client.ClientConnect
import java.awt.EventQueue

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

import org.lxz.tools.syncdevice.helper.HexHelper

import javax.swing.JTextField
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JButton
import javax.swing.JRadioButton
import javax.swing.JTextArea
import javax.swing.JCheckBox
import java.awt.Color
import javax.swing.JScrollPane

import java.util.LinkedList
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.swing.JTextPane

class ClientUI : JFrame() {

    lateinit var et_log: JTextPane
    lateinit var contentPane: JPanel
    lateinit var et_ip: JTextField
    lateinit var et_port: JTextField
    lateinit var et_time: JTextField

    lateinit var btn_send: JButton
    lateinit var rb_content: JRadioButton
    lateinit var rb_hex: JRadioButton
    lateinit var et_content: JTextArea
    lateinit var btn_connect: JButton
    lateinit var et_time_content: JTextArea
    lateinit var label_log: JLabel
    lateinit var rb_time_hex: JRadioButton
    lateinit var rb_time_content: JRadioButton
    lateinit var label_time: JLabel
    lateinit var cb_time: JCheckBox
    lateinit var label_time_send: JLabel

    internal var poolExecutor: ScheduledThreadPoolExecutor? = null

    internal val EVENT_BTN_CONNECT = 1
    internal val EVENT_BTN_SEND = 2
    internal val EVENT_BTN_TIME_SEND = 3
    internal val EVENT_BTN_CLOSE_TIME_SEND = 4

    private var serviceConnect: ClientConnect? = null

    private val messageList = LinkedList<String>()

    private fun onClick(id: Int) {
        when (id) {
            EVENT_BTN_CONNECT -> connect()
            EVENT_BTN_SEND -> sendMessage()
            EVENT_BTN_TIME_SEND -> timeScheduled()
            EVENT_BTN_CLOSE_TIME_SEND -> closeTimeScheduled()
            else -> {
            }
        }
    }

    private fun timeScheduled() {
        var time = 0
        try {
            time = Integer.valueOf(et_time.text)!!
            if (time < 0) {
                showErrorMessage("时间必须大于0")
                cb_time.isSelected = false
                return
            }
        } catch (e: NumberFormatException) {
            cb_time.isSelected = false
            showErrorMessage("输入正确的时间参数")
            return
        }

        if (poolExecutor == null || poolExecutor != null && poolExecutor!!.isShutdown) {
            poolExecutor = ScheduledThreadPoolExecutor(10)
        }
        et_time.isEnabled = false
        poolExecutor!!.scheduleAtFixedRate({
            if (serviceConnect != null) {
                if (rb_time_hex.isSelected) {
                    serviceConnect!!.write(HexHelper.hexStr2Bytes(et_time_content.text))
                } else {
                    serviceConnect!!.write(et_time_content.text)
                }
            }
        }, 0, time.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun closeTimeScheduled() {
        poolExecutor?.shutdownNow()
        poolExecutor = null
        et_time.isEnabled = true
    }


    private fun sendMessage() {
        if (serviceConnect != null) {
            if (rb_hex.isSelected) {
                serviceConnect!!.write(HexHelper.hexStr2Bytes(et_content.text))
            } else {
                serviceConnect!!.write(et_content.text.toString())
            }
        }
    }

    private fun connect() {
        var port = 0
        try {
            port = Integer.valueOf(et_port.text)!!
        } catch (e: NumberFormatException) {
            showErrorMessage("端口必须为数字")
            return
        }

        if (serviceConnect != null) {
            serviceConnect!!.closeConnect()
        } else {
            val connect = ClientConnect(et_ip.text, port)
            connect.setConnectListen(object: ClientConnect.ServiceConnectListen {
                override fun listen(isConnect: Boolean, exce: Exception?) {
                    when (isConnect) {
                        true -> {
                            setUIButtonEnable(true)
                            btn_connect.text = "关闭连接"
                            serviceConnect = connect
                        }
                        false -> {
                            if (exce != null) {
                                showErrorMessage(exce.javaClass.simpleName)
                            }
                            setUIButtonEnable(false)
                            btn_connect.text = "建立连接"
                            serviceConnect = null
                        }
                    }
                }

            }).setDataListen(object : ClientConnect.ReaderDataListen {
                override fun listen(content: String) {
                    log("服务器应答: $content")
                }

            }).setSendListen(object: ClientConnect.SendDataListen {
                override fun listen(content: String) {
                    log("客户端发送: $content")
                }

            }).startServer()

        }

    }

    fun setUIButtonEnable(b: Boolean) {
        btn_send.isEnabled = b
        rb_content.isEnabled = b
        rb_hex.isEnabled = b
        et_content.isEnabled = b
        et_time_content.isEnabled = b
        label_log.isEnabled = b
        rb_time_hex.isEnabled = b
        rb_time_content.isEnabled = b
        label_time.isEnabled = b
        cb_time.isEnabled = b
        label_time_send.isEnabled = b
        et_time.isEnabled = b
    }

    /**
     * Create the frame.
     */
    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setBounds(100, 100, 543, 540)
        title = "压力测试 模拟TCP设备连接简易工具"
        contentPane = JPanel()
        contentPane.border = EmptyBorder(5, 5, 5, 5)
        setContentPane(contentPane)
        contentPane.layout = null

        et_ip = JTextField()
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

        btn_send = JButton("发送")
        btn_send.isEnabled = false
        btn_send.setBounds(419, 78, 96, 71)
        btn_send.addActionListener { onClick(EVENT_BTN_SEND) }
        contentPane.add(btn_send)

        rb_content = JRadioButton("字符串")
        rb_content.isEnabled = false
        rb_content.setBounds(312, 90, 81, 23)
        rb_content.isSelected = true
        rb_content.addActionListener {
            rb_hex.isSelected = false
            rb_content.isSelected = true
        }
        contentPane.add(rb_content)

        rb_hex = JRadioButton("hex发送")
        rb_hex.isEnabled = false
        rb_hex.setBounds(312, 114, 81, 23)
        rb_hex.addActionListener {
            rb_hex.isSelected = true
            rb_content.isSelected = false
        }
        contentPane.add(rb_hex)

        et_content = JTextArea()
        et_content.setBounds(30, 78, 270, 71)
        contentPane.add(et_content)

        btn_connect = JButton("建立连接")
        btn_connect.addActionListener { onClick(EVENT_BTN_CONNECT) }
        btn_connect.setBounds(419, 24, 96, 40)
        contentPane.add(btn_connect)

        et_time_content = JTextArea()
        et_time_content.setBounds(30, 167, 270, 71)
        contentPane.add(et_time_content)

        label_log = JLabel("打印日志")
        label_log.isEnabled = false
        label_log.setBounds(29, 250, 61, 16)
        contentPane.add(label_log)

        rb_time_hex = JRadioButton("hex发送")
        rb_time_hex.isEnabled = false
        rb_time_hex.setBounds(312, 185, 81, 23)
        rb_time_hex.addActionListener {
            rb_time_hex.isSelected = true
            rb_time_content.isSelected = false
        }

        contentPane.add(rb_time_hex)

        rb_time_content = JRadioButton("字符串")
        rb_time_content.isEnabled = false
        rb_time_content.isSelected = true
        rb_time_content.setBounds(312, 161, 81, 23)
        rb_time_content.addActionListener {
            rb_time_hex.isSelected = false
            rb_time_content.isSelected = true
        }
        contentPane.add(rb_time_content)

        et_time = JTextField()
        et_time.isEnabled = false
        et_time.setBounds(312, 211, 60, 26)
        contentPane.add(et_time)
        et_time.columns = 10

        label_time = JLabel("毫秒")
        label_time.isEnabled = false
        label_time.setBounds(378, 216, 32, 16)
        contentPane.add(label_time)

        cb_time = JCheckBox("开启")
        cb_time.isEnabled = false
        cb_time.foreground = Color.BLACK
        cb_time.toolTipText = ""
        cb_time.setBounds(442, 185, 61, 40)
        cb_time.addActionListener {
            if (cb_time.isSelected) {
                timeScheduled()
            } else {
                closeTimeScheduled()
            }
        }
        contentPane.add(cb_time)

        label_time_send = JLabel("定时发送")
        label_time_send.isEnabled = false
        label_time_send.setBounds(442, 167, 61, 16)
        contentPane.add(label_time_send)


        val scrollPane_1 = JScrollPane()
        scrollPane_1.setBounds(30, 276, 485, 220)
        contentPane.add(scrollPane_1)
        et_log = JTextPane()
        scrollPane_1.setViewportView(et_log)
    }

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
                val frame = ClientUI()
                frame.setLocationRelativeTo(null)
                frame.isVisible = true
            }
        }
    }
}
