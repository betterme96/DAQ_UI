package com.wzb.service;

import com.wzb.util.RingBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class ReadOuter implements Runnable{
    public volatile boolean exit = false;

    private Socket dataSocket;//socket实例
    private RingBuffer curBuffer;//数据要写入的下一级buffer

    //构造函数，初始化socket和buffer
    public ReadOuter(Socket dataSocket, RingBuffer ringBuffer){
        this.dataSocket = dataSocket;
        this.curBuffer = ringBuffer;
    }
    public void TcpReadBeforeConfig() throws IOException {
        InputStream in = dataSocket.getInputStream();//获取socket的输入流，用于接收对端数据

        byte[] data = new byte[1024];//数据接收缓冲
        int len = 0;//每次从socket读取的数据量
        int total = 0;//总的数据接收量
        System.out.println("1111");
        try{
            while ((len = in.read(data)) != -1){
                System.out.println("data recv:" + len);
                total += len;
            }
        }catch (SocketTimeoutException e) {
            System.out.println("time out!");
        }

        System.out.println("before config total recv:" + total);
    }

    @Override
    public void run() {
        try{
            //socket接收缓存设置
            //dataSocket.setReceiveBufferSize(32*1024);

            System.out.println("Readout module working......");
            InputStream in = dataSocket.getInputStream();
            byte[] data = new byte[1024*5000];
            int length = 0;
            long total = 0;
            long startTime=System.currentTimeMillis();
            try{
                while((length = in.read(data)) != -1){
                    /*
                    if(exit){
                        curBuffer.write(data, 0,length, "ReadOut");
                        //System.out.println("socket in len:" + length);
                        total += length;
                        break;
                    }
                    int write = curBuffer.write(data, 0,length, "ReadOut");

                     */
                    total += length;
                    /*
                    if(write == -1){
                        break;
                    }

                     */
                }
                long time = (System.currentTimeMillis() - startTime)/1000;
                System.out.println(total/ time);
            }catch (SocketTimeoutException e){
                System.out.println("readout over time!!");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}




