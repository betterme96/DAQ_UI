package com.wzb.service;

import com.wzb.helper.RingBuffer;

public class Builder implements Runnable{
    private RingBuffer curBuffer;
    private RingBuffer nextBuffer;
    private Store store;

    public volatile boolean start = false;
    public volatile boolean exit = false;

    public Builder(RingBuffer curBuffer, RingBuffer nextBuffer, Store store){
        this.curBuffer = curBuffer;
        this.nextBuffer = nextBuffer;
        this.store = store;
    }

    public void run() {
        try{
            while (!exit){
                while (!start && !exit){
                    Thread.sleep(2000);
                    //System.out.println("builder check start");
                }
                if(start){
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
                    start = false;
                    System.out.println("build data suc!!");
                }
            }
            System.out.println("build module Thread shut down");
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