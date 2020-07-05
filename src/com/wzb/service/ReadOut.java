package com.wzb.service;

import com.wzb.helper.RingBuffer;

import java.io.InputStream;
import java.net.Socket;


public class ReadOut implements Runnable{
    public volatile boolean start = false;
    public volatile boolean exit = false;

    private String ip;
    private String port;
    private RingBuffer curBuffer;

    private Builder builder;
    public ReadOut(String ip, String port, RingBuffer ringBuffer, Builder builder){
        this.ip = ip;
        this.port = port;
        this.curBuffer = ringBuffer;
        this.builder = builder;
    }

    public void run() {
        try{
            Socket dataSocket = new Socket(ip, Integer.parseInt(port));
            while (!exit){
                while (!start && !exit){
                    Thread.sleep(2000);
                   // System.out.println("readout check start");
                }
                if(start){
                    System.out.println("Readout module working......");
                    InputStream in = dataSocket.getInputStream();

                    byte[] data = new byte[100];
                    int length = 0;
                    while((length = in.read(data)) != -1){
                        //System.out.println("----readout write----");
                        int write = curBuffer.write(data, 0,length, "ReadOut");
                        if(write == -1){
                            break;
                        }
                    }
                    start = false;
                    System.out.println("readout suc!");
                }
            }
            System.out.println("Readout module Thread shut down");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}




