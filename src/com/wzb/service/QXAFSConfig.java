package com.wzb.service;

import com.wzb.interfaces.Config;
import com.wzb.util.StringOp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

public class QXAFSConfig implements Config {
    private OutputStream dataOut;//用于发送配置
    private InputStream dataIn;//用于接收配置回包

    public volatile boolean configSuc = false;

    public QXAFSConfig(Socket commSocket) throws IOException {
        this.dataOut = commSocket.getOutputStream();
        this.dataIn = commSocket.getInputStream();
    }

    @Override
    public void work(List<String> configs) throws IOException, InterruptedException {
        //发送复位指令
        resetSend();

        Thread.sleep(1000);

        //发送配置之前，将电子学板上的残留数据读空
        TcpReadBeforeConfig(dataIn);

        //启动配置发送和配置接收线程
        QXAFSSendConfig commSend = new QXAFSSendConfig(dataOut, configs);
        QXAFSRecvConfig commRecv = new QXAFSRecvConfig(dataIn, commSend, configs.size());
        Thread t1 = new Thread(commSend);
        Thread t2 = new Thread(commRecv);
        t1.start();
        t2.start();

        while (!(commSend.sendSuc && commRecv.done)){
            System.out.println("wait config");
            Thread.sleep(1000);
        }
    }

    @Override
    public void sendStart() throws IOException {
        byte[] start = new byte[8];
        Arrays.fill(start, (byte)0x00);
        start[7] = 0x55;
        start[1] = (byte)0x84;
        start[0] = (byte)0xaa;
        dataOut.write(start);
    }

    private void resetSend() throws IOException {
        byte[] reset = new byte[8];
        Arrays.fill(reset, (byte) 0x00);
        reset[7] = 0x55;
        reset[1] = (byte)0x81;
        reset[0] = (byte) 0xaa;
        dataOut.write(reset);
    }

    private void TcpReadBeforeConfig(InputStream dataIn) throws IOException {
        //System.out.println("read before send config");
        byte[] data = new byte[16000];
        int len = 0;
        long total = 0;
        try{
            while ((len = dataIn.read(data)) != -1){
                System.out.println("len:" + len);
                total += len;
            }
        }catch (SocketTimeoutException e){
            System.out.println("read before send config run time out");
        }finally {
            System.out.println(total + " bytes recv");
        }
    }
}
class QXAFSSendConfig implements Runnable{
    private OutputStream commOut;
    private List<String> configs;
    private int channel;

    public volatile boolean sendSuc = false;
    public volatile boolean recvSuc = false;
    public volatile boolean recvStart = false;
    public volatile int recvNum = -1;

    public QXAFSSendConfig(OutputStream commOut, List<String> configs) throws IOException {
        this.commOut = commOut;
        this.configs = configs;
        this.channel = Integer.parseInt(configs.get(configs.size()-1));
        configs.remove(configs.size()-1);
    }

    @Override
    public void run() {
        try {
            recvStart = true;
            //可用配置指令的索引从0开始
            int totalConfigNum = configs.size();
            int curIdx = 0;
            //持续发送文件中的命令，直至收到最后一条命令的确认
            while(!recvSuc){
                //当前指令序号小于指令总数，才可以读文件
                if(curIdx < totalConfigNum){
                    //System.out.println("send config:" + configs.get(curIdx));
                    byte[]  config = StringOp.string2hex(configs.get(curIdx));
                    commOut.write(config);
                    ///commOut.write(configs.get(curIdx).getBytes());
                    curIdx++;
                }
            }

            //指令发送结束
            stopSend();
            //通道使能（通道使能参数放在configs的最后一个元素处）
            channelEnableSend();

            System.out.println("send over");

            this.sendSuc = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void channelEnableSend() throws IOException {
        byte[] enable = new byte[8];
        Arrays.fill(enable, (byte) 0x00);
        enable[7] = 0x55;
        enable[6] = (byte)channel;
        //enable[6] = 0x03;//通道使能
        enable[1] = (byte)0x82;
        enable[0] = (byte) 0xaa;
        commOut.write(enable);
       // System.out.println("channel enable");
    }

    private void stopSend() throws IOException {
        byte[] stop = new byte[8];
        Arrays.fill(stop, (byte) 0x00);
        stop[7] = 0x55;
        stop[1] = (byte)0x83;
        stop[0] = (byte) 0xaa;
        commOut.write(stop);
    }

}

class QXAFSRecvConfig implements Runnable {
    private InputStream commIn;
    private QXAFSSendConfig commSend;
    private int totalCommSize;
    public volatile boolean done = false;
    public QXAFSRecvConfig(InputStream commIn, QXAFSSendConfig commSend, int totalCommSize){
        this.commIn = commIn;
        this.commSend = commSend;
        this.totalCommSize = totalCommSize;
    }

    @Override
    public void run() {
        byte[] config = new byte[8];
        int zeroCount = 0;
        int totalCount = 0;
        while (true){
            try {
                while (!commSend.recvStart){
                    Thread.sleep(100);
                }

                commIn.read(config);
                totalCount++;
                //System.out.println("recv config:" + StringOp.byte2string(config));

                commSend.recvNum = getSeq(config);
                //System.out.println("recvNum:" + commSend.recvNum + " totalNum:" + totalCommSize);
                if(commSend.recvNum == totalCommSize){
                    //System.out.println("----");
                    commSend.recvSuc = true;
                }


                if(commSend.recvNum == 0){
                    zeroCount = zeroCount + 1;
                }

                //System.out.println("zeroCount:" + zeroCount);
                if(zeroCount == 2 && totalCount == totalCommSize+2){
                    System.out.println("total config count :" + totalCount);
                    break;
                }
                //System.out.println("totalSize: " + totalCommSize);

            } catch (IOException e) {
                //System.out.println("read time out");
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("recv over");
        Thread.interrupted();
        done = true;
    }

    private int getSeq(byte[] config) {
        //回传指令的32-47位代表指令序号
        //对应byte数组的第2、3
        //System.out.println(Integer.toHexString(config[2]) + "  " + Integer.toHexString(config[3]));
        return ((config[2] & 0xff) << 8 | config[3] & 0xff) & 0xffff;
    }
}