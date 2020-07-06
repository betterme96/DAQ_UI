package com.wzb.service;

import com.wzb.helper.RingBuffer;

public class Builder implements Runnable{
    private RingBuffer curBuffer;
    private RingBuffer nextBuffer;

    public volatile boolean exit = false;

    public Builder(RingBuffer curBuffer, RingBuffer nextBuffer){
        this.curBuffer = curBuffer;
        this.nextBuffer = nextBuffer;
    }

    public void run() {
        try{
            System.out.println("Builder module working......");

            byte[] data = new byte[100];
            int length = 0;
            while ((length = curBuffer.read(data, 0, 100,"builder")) != -1){
                //System.out.println("----builder read----");
                int write = handleData(data,length);
                if(write == -1) {
                    break;
                }
            }
            System.out.println("build data suc!!");
            while (!exit){
                Thread.sleep(1000);
            }
            System.out.println("build module Thread shut down!");
        }catch (Exception e){

        }

    }

    private int handleData(byte[] data, int length) throws InterruptedException {
        /*
        check data
         */
        //if data meet the conditions, write to next buffer
        return nextBuffer.write(data,0,length, "Builder module");
        //System.out.println("----builder write----");

    }
}