package com.wzb.helper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {
    public static String getCurDate(){
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyyMMdd");
        return sdf.format(new Date());
    }
    public static String getCurTime(){
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
