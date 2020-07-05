package com.wzb.service;

import com.wzb.helper.RingBuffer;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class Store implements Runnable{
    private RingBuffer curBuffer;
    public String fileName;

    public volatile boolean start = false;
    public volatile boolean exit = false;

    public Store(RingBuffer curBuffer){
        this.curBuffer = curBuffer;
    }

    public void run() {
        try {
            while (!exit){
                while (!start && !exit){
                    Thread.sleep(2000);
                   // System.out.println("store check start");
                }
                if(start){
                    System.out.println("store filename:" + fileName);
                    String filePath = "./result/RUN" + fileName + "-RawData.dat";
                    File sFile = new File(filePath);
                    if(!sFile.exists()){
                        sFile.createNewFile();
                    }
                    FileOutputStream storeFile = new FileOutputStream(sFile);
                    System.out.println("Store module working......");
                    byte[] data = new byte[100];
                    int length = 0;
                    while((length = curBuffer.read(data,0,100, "store") )!= -1){
                        storeFile.write(data, 0, length);
                    }
                    start = false;
                    storeFile.flush();;
                    storeFile.close();
                    System.out.println("store suc!!");
                }
            }
            System.out.println("Store module Thread shut down");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
