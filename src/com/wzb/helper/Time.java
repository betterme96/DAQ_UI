package com.wzb.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {

    public static String getCurTime(){
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    public static String getDiffTime(String before, String after) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date d1 = sdf.parse(before);
        Date d2 = sdf.parse(after);
        //millisecond
        long diffTime = d2.getTime() - d1.getTime();
        long day = diffTime/(24*60*60*1000);
        long hour = diffTime/(60*60*1000) - day*24;
        long min = diffTime/(60*1000) - day*24*60 - hour*60;
        long second = diffTime/1000 - day*24*60*60 - hour*60*60 - min*60;

        StringBuilder res = new StringBuilder();
        if(day > 0){
            res.append(String.format("%02d", day));
            res.append(" ");
        }

        res.append(String.format("%02d", hour));
        res.append(":");


        res.append(String.format("%02d", min));;
        res.append(":");
        res.append(String.format("%02d", second));
        return res.toString();
    }
}
