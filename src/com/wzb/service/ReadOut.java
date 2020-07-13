package com.wzb.service;

import com.wzb.util.RingBuffer;

import java.io.InputStream;
import java.net.Socket;


public class ReadOut implements Runnable{
    public volatile boolean exit = false;

    private Socket dataSocket;
    private RingBuffer curBuffer;

    public ReadOut(Socket dataSocket, RingBuffer ringBuffer){
        this.dataSocket = dataSocket;
        this.curBuffer = ringBuffer;
    }

    public void run() {
        try{

            System.out.println("Readout module working......");
            InputStream in = dataSocket.getInputStream();

            byte[] data = new byte[1024*10];
            int length = 0;
            long total = 0;
            while((length = in.read(data)) != -1){
                System.out.println("socket in len:" + length);
                if(exit){
                    curBuffer.write(data, 0,length, "ReadOut");
                    System.out.println("socket in len:" + length);
                    total += length;
                    break;
                }
                //System.out.println("----readout write----");
                int write = curBuffer.write(data, 0,length, "ReadOut");
                total += length;
                if(write == -1){
                    break;
                }
            }
            System.out.println("readout suc!");
            System.out.println("readout len:" + total);
            System.out.println("readout module Thread shutdown!");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}




