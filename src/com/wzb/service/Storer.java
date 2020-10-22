package com.wzb.service;

import com.wzb.util.RingBuffer;

import java.io.File;
import java.io.FileOutputStream;

public class Storer implements Runnable{
    private RingBuffer curBuffer;
    private String fileName;

    public volatile boolean exit = false;

    public Storer(RingBuffer curBuffer){
        this.curBuffer = curBuffer;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void run() {
        try {
            System.out.println("Store module working......");
            //System.out.println("store filename:" + fileName);
            File sFile = new File(fileName);
            if(!sFile.exists()){
                sFile.createNewFile();
            }
            FileOutputStream storeFile = new FileOutputStream(sFile);
            byte[] data = new byte[1024*500];
            int length = 0;
            long total = 0;
            while((length = curBuffer.read(data,0,data.length, "store") )!= -1){
                storeFile.write(data, 0, length);
                total += length;
            }
            storeFile.flush();;
            storeFile.close();
           // System.out.println("store suc!!");
            System.out.println("file len:" + total);
            while (!exit){
                Thread.sleep(1000);
            }
            System.out.println("store module Thread shut down!");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
