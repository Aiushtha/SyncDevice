package org.lxz.tools.syncdevice.helper;

import org.joda.time.DateTime;

/**
 * Created by linxingzhu on 2018/7/2.
 */
public class TimeHelper {
    public static final String DEFAULT_DATE_FORMAT="yyyy-MM-dd HH:mm:ss";
    public static String getNowTime(){

        return new DateTime().toString(DEFAULT_DATE_FORMAT);
    }

}
