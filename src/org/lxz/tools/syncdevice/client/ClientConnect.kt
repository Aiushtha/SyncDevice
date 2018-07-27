package org.lxz.tools.syncdevice.client

import java.io.*
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Created by linxingzhu on 2018/6/28.
 */
class ClientConnect(val host: String, val port: Int) : Runnable {

    private var socket: Socket? = null
    private var mConnectListen: ServiceConnectListen? = null

    private var mDataListen: ReaderDataListen? = null
    private var mSendListen: SendDataListen? = null

    private var outStream: OutputStream? = null
    private var printWriter: PrintWriter? = null
    private var streamReader: InputStreamReader? = null
    private val exeService = Executors.newSingleThreadExecutor()
    private var reader: BufferedReader? = null


    interface ReaderDataListen {
        fun listen(content: String)
    }

    interface SendDataListen {
        fun listen(content: String)
    }

    interface ServiceConnectListen {
        fun listen(connect: Boolean, exce: Exception?)
    }


    fun closeConnect() {
        if (reader != null) {
            try {
                socket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                reader!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    override fun run() {
        val chars = CharArray(512)
        var len: Int=-1
        var content: String? = null
        try {
            fun read():Int{
              len=reader!!.read(chars)
              return len
            }
            while (socket!=null&&!socket!!.isClosed&&read() != -1) {
                content = String(chars, 0, len)
                println("接收:$content")
                if (mDataListen != null) {
                    mDataListen!!.listen(content)
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()

        }

        if (mConnectListen != null) {
            mConnectListen!!.listen(false, null)
        }
    }

    /**监听读取数据 */
    fun read() {
        Thread(this).start()
    }


    fun startServer() {
        try {
            socket = Socket(host, port)
            outStream = socket!!.getOutputStream()
            printWriter = PrintWriter(outStream!!)
            streamReader = InputStreamReader(socket!!.getInputStream())
            reader = BufferedReader(streamReader!!)
            if (mConnectListen != null) {
                mConnectListen!!.listen(true, null)
            }
            read()

        } catch (e: Exception) {
            e.printStackTrace()
            if (mConnectListen != null) {
                mConnectListen!!.listen(false, e)
            }
        }

    }


    fun getConnectListen(): ServiceConnectListen? {
        return mConnectListen
    }

    fun setConnectListen(connectListen: ServiceConnectListen): ClientConnect {
        this.mConnectListen = connectListen
        return this
    }

    fun getDataListen(): ReaderDataListen? {
        return mDataListen
    }

    fun getSendListen(): SendDataListen? {
        return mSendListen
    }

    fun setSendListen(sendListen: SendDataListen): ClientConnect {
        this.mSendListen = sendListen
        return this
    }

    fun setDataListen(dataListen: ReaderDataListen): ClientConnect {
        this.mDataListen = dataListen
        return this
    }

    fun write(s: String) {
        if (socket!!.isConnected) {
            if (printWriter != null) {
                printWriter!!.write(s)
                printWriter!!.flush()
                if (mSendListen != null) {
                    mSendListen!!.listen(s)
                }
            }
        }
    }

    fun write(s: ByteArray) {
        if (socket!!.isConnected) {
            if (outStream != null) {
                try {
                    outStream!!.write(s)
                    outStream!!.flush()
                    mSendListen!!.listen(String(s))
                } catch (e: IOException) {
                    e.printStackTrace()
                }


            }
        }
    }




}
