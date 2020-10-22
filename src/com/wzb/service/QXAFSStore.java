package com.wzb.service;

import com.wzb.interfaces.Store;
import com.wzb.util.FileOp;
import com.wzb.util.RingBuffer;


import java.io.FileOutputStream;
import java.io.IOException;

public class QXAFSStore implements Runnable {
    private RingBuffer ringBuffer;
    private FileOutputStream analyseDataFile;

    public volatile boolean exit = false;
    public volatile boolean over = false;
    public QXAFSStore(RingBuffer ringBuffer){
        this.ringBuffer = ringBuffer;
    }

    public void setAnalyseDataFile(String fileName) throws IOException {
        this.analyseDataFile = FileOp.createFileOutputStream(fileName + ".txt");
    }
    @Override
    public void run(){
        System.out.println("Store module working......");
        byte[] data = new byte[5000000];
        int len = 0;
        try{
            analyseDataFile.write("pulse adc1 adc2\n".getBytes());
            while ((len = ringBuffer.read(data, 0, data.length, "Store Module") )!= -1){
                analyseDataFile.write(data, 0, len);
            }
            analyseDataFile.flush();
            analyseDataFile.close();
            over = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
