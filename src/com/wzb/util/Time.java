package com.wzb.util;

import javafx.application.Platform;
import javafx.scene.control.TextField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Time {

    public static String getSumTime(String t1, String t2) throws ParseException {
        StringBuilder res = new StringBuilder();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
        Date d1 = sdf1.parse(t1);
        Date d2 = sdf2.parse(t2);
        long sum = d1.getTime() + d2.getTime();
        return res.toString();
    }
    public static String getCurTime(){
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    /*
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
     */

    public static Timer activeTimeShow(TextField text_active_time){
        Timer timer = new Timer();
        text_active_time.setText("00:00:00");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                        try {
                            Date date = sdf.parse(text_active_time.getText());
                            long cur = date.getTime()+1000;
                            date.setTime(cur);
                            text_active_time.setText(sdf.format(date));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        },0,1000);
        return timer;
    }
}
