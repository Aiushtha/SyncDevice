package org.lxz.tools.syncdevice.bean;

/**
 * Created by linxingzhu on 2018/7/2.
 */
public class StatusBean {
    /**主板id*/
    public String lockId;
    /**想要开锁的通道*/
    public String channelId;
    /**请求发起的时间*/
    public String createTime;
    /**0待命状态,1开锁成功,-1开锁失败*/
    public int code;
}
