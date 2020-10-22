package com.wzb.service;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import com.wzb.util.StringOp;

public class Configer {
    private Socket commSocket;
    private DatagramSocket commUdpSocket;
    private InetSocketAddress peerAddr;

    public Configer(Socket commSocket){
        this.commSocket = commSocket;
    }
    public Configer(DatagramSocket commUdpSocket, InetSocketAddress peerAddr){
        this.commUdpSocket = commUdpSocket;
        this.peerAddr = peerAddr;
    }
    public void sendQXAFSConfig(List<String> configs, int channel) throws InterruptedException {
        SendConfig sendConfig = new SendConfig(commUdpSocket, peerAddr, configs, channel);
        RecvConfig recvConfig = new RecvConfig(commUdpSocket, sendConfig, configs.size());
        new Thread(sendConfig).start();
        new Thread(recvConfig).start();
        while (!sendConfig.sendSuc){
            Thread.sleep(1000);
        }
    }
    public void sendQXAFSStart() throws IOException {
        byte[] start = new byte[8];
        Arrays.fill(start, (byte) 0x00);
        start[7] = 0x55;
        start[1] = 0x07;
        start[0] = (byte) 0xaa;
        DatagramPacket packet = new DatagramPacket(start, start.length, peerAddr);
        commUdpSocket.send(packet);
    }

    public void sendEDConfig() throws InterruptedException, IOException {
        OutputStream socketOut = commSocket.getOutputStream();
        FileInputStream configIn = new FileInputStream(new File("./daqFile/config/ED-config.txt"));
        BufferedReader br = new BufferedReader(new InputStreamReader(configIn));
        String config = "";
        System.out.println("Sending config......");
        while((config = br.readLine()) != null){
            byte[] data = StringOp.string2hex(config);
            socketOut.write(data);
            Thread.sleep(1000);
        }
        configIn.close();
        br.close();
        System.out.println("Sending config suc!");
    }

}

class SendConfig implements Runnable{
    private DatagramSocket sendCommSocket;
    private InetSocketAddress peerAddr;
    private List<String> configs;
    private int channel;

    public volatile boolean sendSuc = false;
    public volatile int recvNum = -1;
    public volatile  boolean isFull = false;
    public SendConfig(DatagramSocket sendCommSocket, InetSocketAddress peerAddr, List<String> configs, int channel){
        this.sendCommSocket = sendCommSocket;
        this.peerAddr = peerAddr;
        this.configs = configs;
        this.channel = channel;
    }
    @Override
    public void run() {
        try {
            //全局复位
            sendReset(sendCommSocket, peerAddr);


            //可用配置指令的索引从0开始
            int totalConfigNum = configs.size();

            int curIdx = 0;
            //持续发送文件中的命令，直至收到最后一条命令的确认
            while(recvNum < totalConfigNum){
                //System.out.println("recv num:" + recvNum);
                if(recvNum == totalConfigNum){
                    break;
                }
                //当前指令序号小于指令总数，才可以读文件
                if(curIdx < totalConfigNum){;
                    //System.out.println(curIdx + "    " +configs.get(curIdx));
                    byte[]  config = StringOp.string2hex(configs.get(curIdx));
                    DatagramPacket sendPacket = new DatagramPacket(config, config.length, peerAddr);
                    sendCommSocket.send(sendPacket);
                    //System.out.println(configs.get(curIdx));
                    curIdx++;
                }else{
                    Thread.sleep(2000);
                    break;
                }


                /*
                //如果电子学没有可用空间来接收配置指令,暂停指令发送
                if(isFull){
                    Class.class.wait();
                }

                //线程被唤醒后，保存非满状态
                isFull = false;

                 */

                //线程被唤醒后，重新检查电子学发来的回传指令中的指令序号
                //若电子学已经接收到全部指令，则结束配置文件中的指令发送

                //System.out.println("total:" + totalConfigNum + "   recv:" + recvNum);

                //接收到电子学的回传指令，则从该回传指令中的指令序号开始传输
                /*
                if(isRecv){
                    curIdx = recvNum;
                    isRecv = false;
                }else if(curIdx < totalConfigNum){
                    //System.out.println("curIdx++");
                    curIdx++;
                }else {
                    Thread.sleep(1000);
                }

                 */


            }
            //System.out.println("total:" + totalConfigNum + "   recv:" + recvNum);
            //指令发送结束
            sendStop(sendCommSocket, peerAddr);
            //通道使能
            sendChannelEnable(sendCommSocket, peerAddr, channel);
            Thread.sleep(2000);
            System.out.println("send config suc!!");
            this.sendSuc = true;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendStop(DatagramSocket sendCommSocket, InetSocketAddress peerAddr) throws IOException {
        byte[] stop = new byte[8];
        Arrays.fill(stop, (byte) 0x00);
        stop[7] = 0x55;
        stop[1] = 0x05;
        stop[0] = (byte) 0xaa;
        DatagramPacket packet = new DatagramPacket(stop, stop.length, peerAddr);
        sendCommSocket.send(packet);
    }

    private void sendChannelEnable(DatagramSocket sendCommSocket, InetSocketAddress peerAddr, int channel) throws IOException {
        byte[] enable = new byte[8];
        Arrays.fill(enable, (byte) 0x00);
        enable[7] = 0x55;
        enable[6] = 0x03;
        enable[1] = 0x06;
        enable[0] = (byte) 0xaa;
        DatagramPacket packet = new DatagramPacket(enable, enable.length, peerAddr);
        sendCommSocket.send(packet);
        //System.out.println("send channel enable");
    }

    private void sendReset(DatagramSocket sendCommSocket, InetSocketAddress peerAddr) throws IOException {
        byte[] reset = new byte[8];
        Arrays.fill(reset, (byte) 0x00);
        reset[7] = 0x55;
        reset[1] = 0x08;
        reset[0] = (byte) 0xaa;
        DatagramPacket packet = new DatagramPacket(reset, reset.length, peerAddr);
        sendCommSocket.send(packet);
    }
}

class RecvConfig implements Runnable{
    private DatagramSocket recvCommSocket;
    private SendConfig sendComm;
    private int totalConfigNum;

    public RecvConfig(DatagramSocket recvCommSocket, SendConfig sendComm, int totalConfigNum){
        this.recvCommSocket = recvCommSocket;
        this.sendComm = sendComm;
        this.totalConfigNum = totalConfigNum;
    }
    @Override
    public void run() {
        try{
            byte[] config = new byte[8];
            while (true){
                DatagramPacket recvPacket = new DatagramPacket(config, config.length);
                recvCommSocket.receive(recvPacket);
                /*
                //若无剩余空间，则通知发送进程休眠
                if(getSpace(config) == 0){
                    sendComm.isFull = true;
                }else if(sendComm.isFull){
                    //若有剩余空间，且发送线程休眠，则将其唤醒
                    Class.class.notify();
                }
                 */
                //System.out.println("recv config:" + StringOp.byte2string(config));
                /*
                for(int i = sendComm.recvNum+1; i < curNum; ++i){
                    //System.out.println("lost recv config :" + i);
                }

                 */
                sendComm.recvNum = getSeq(config);
                if(sendComm.recvNum == totalConfigNum){
                    break;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private int getSpace(byte[] config) {
        int space = (config[3] << 16 | config[2] << 8 | config[1]) & 0xffffff;
        return space;
    }

    private int getSeq(byte[] config) {
        //回传指令的32-47位代表指令序号
        //对应byte数组的第2、3
        //System.out.println(Integer.toHexString(config[2]) + "  " + Integer.toHexString(config[3]));
        return ((config[2] & 0xff) << 8 | config[3] & 0xff) & 0xffff;
    }
}