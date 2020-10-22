package com.wzb.service;

import com.wzb.util.RingBuffer;

public class Analyser implements Runnable{
    private RingBuffer curBuffer;
    private RingBuffer nextBuffer;

    public volatile boolean exit = false;

    public Analyser(RingBuffer curBuffer, RingBuffer nextBuffer){
        this.curBuffer = curBuffer;
        this.nextBuffer = nextBuffer;
    }

    public void run() {
        try{
            System.out.println("Builder module working......");

            byte[] data = new byte[1024*500];
            int length = 0;
            long total = 0;
            while ((length = curBuffer.read(data, 0, data.length,"builder")) != -1){
                //System.out.println("----builder read----");
                int write = handleData(data,length);
                total += length;
                if(write == -1) {
                    break;
                }
            }
            //System.out.println("build data suc!!");
            //System.out.println("build data len:" + total);
            while (!exit){
                Thread.sleep(1000);
            }
            //System.out.println("build module Thread shut down!");
        }catch (Exception e){

        }

    }

    private int handleData(byte[] data, int length) throws InterruptedException {
        /*
        check data
         */
        //if data meet the conditions, write to next buffer
        return nextBuffer.write(data,0,length, "Builder");
        //System.out.println("----builder write----");

    }
}