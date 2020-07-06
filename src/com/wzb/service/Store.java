package com.wzb.service;

import com.wzb.helper.RingBuffer;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class Store implements Runnable{
    private RingBuffer curBuffer;
    private FileOutputStream storeFile;

    public volatile boolean exit = false;

    public Store(RingBuffer curBuffer){
        this.curBuffer = curBuffer;
    }

    public void createStoreFile(String fileName) throws IOException {
        System.out.println("store filename:" + fileName);
        File sFile = new File(fileName);
        if(!sFile.exists()){
            sFile.createNewFile();
        }
        storeFile = new FileOutputStream(sFile);
    }

    public void run() {
        try {

            System.out.println("Store module working......");
            byte[] data = new byte[100];
            int length = 0;
            while((length = curBuffer.read(data,0,100, "store") )!= -1){
                storeFile.write(data, 0, length);
            }
            storeFile.flush();;
            storeFile.close();
            System.out.println("store suc!!");
            while (!exit){
                Thread.sleep(1000);
            }
            System.out.println("store module Thread shut down!");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
