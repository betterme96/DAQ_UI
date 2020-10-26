package com.wzb.service;

import com.wzb.interfaces.ReadOut;
import com.wzb.util.FileOp;
import com.wzb.util.RingBuffer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class QXAFSReadOut implements Runnable{
    private RingBuffer ringBuffer;
    private FileOutputStream rawDataFile;
    private InputStream dataIn;

    public volatile boolean exit = false;
    public volatile boolean over = false;

    public QXAFSReadOut(Socket dataSocket, RingBuffer ringBuffer) throws IOException {
        this.ringBuffer = ringBuffer;
        this.dataIn = dataSocket.getInputStream();
    }

    public void setRawDataFile(String fileName) throws IOException {
        this.rawDataFile = FileOp.createFileOutputStream(fileName+".dat");
    }

    @Override
    public void run(){
        System.out.println("Readout module working......");
        try{
            //start命令的回包，丢掉
            byte[] start = new byte[8];
            dataIn.read(start);


            byte[] data = new byte[64000000];

            int len = 0;
            while ((len = dataIn.read(data, 0, data.length)) != -1){
                //System.out.println("len :" + len);
                rawDataFile.write(data, 0, len);
                //如果系统进行stop操作，则要对特定的空包进行判定
                /*
                if(exit){
                    ringBuffer.write(data, 0, len, "ReadOut");
                    break;
                }

                 */

                int wirte = ringBuffer.write(data, 0, len, "ReadOut");

                //写超时，数据读出部分阻塞，进行提示
                if(wirte == -1){
                    System.out.println("ReadOut data block!");
                }
            }
            //将文件输出流缓冲区的数据强制写到文件中
            rawDataFile.flush();
            rawDataFile.close();

            over = true;
        }catch (SocketTimeoutException e){

        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }
}
