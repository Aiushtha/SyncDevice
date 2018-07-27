package org.lxz.tools.syncdevice.client

import org.lxz.tools.syncdevice.event.*

/**
 * Created by linxingzhu on 2018/7/24.
 */
var LocadEventClass= arrayListOf(
        HeartbeatEventTask::class.java,
        SearchConnectListEventTask::class.java,
        IsLiveEventTask::class.java,
        SendDeviceEventTask::class.java,
        ReceiveDeviceEventTask::class.java
)