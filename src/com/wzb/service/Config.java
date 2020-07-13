package com.wzb.service;


import java.io.*;
import java.net.Socket;

public class Config {
    private Socket commSocket;

    public Config(Socket commSocket){
        this.commSocket = commSocket;
    }

    public void sendEDConfig() throws InterruptedException, IOException {
        OutputStream socketOut = commSocket.getOutputStream();
        FileInputStream configIn = new FileInputStream(new File("./daqFile/config/ED-config.txt"));
        BufferedReader br = new BufferedReader(new InputStreamReader(configIn));
        String config = "";
        System.out.println("Sending config......");
        while((config = br.readLine()) != null){
            byte[] data = string2hex(config);
            socketOut.write(data);
            Thread.sleep(1000);
        }
        configIn.close();
        br.close();
        System.out.println("Sending config suc!");
    }

    private byte[] string2hex(String s) {
        int num1 = Integer.parseInt(s.substring(0,8),16);
        int num2 = Integer.parseInt(s.substring(8,16),16);
        byte[] data = new byte[8];
        data[0] = (byte)((num1 >> 24) & 0xff);
        data[1] = (byte)((num1 >> 16) & 0xff);
        data[2] = (byte)((num1 >> 8) & 0xff);
        data[3] = (byte)((num1) & 0xff);
        data[4] = (byte)((num2 >> 24) & 0xff);
        data[5] = (byte)((num2 >> 16) & 0xff);
        data[6] = (byte)((num2 >> 8) & 0xff);
        data[7] = (byte)((num2) & 0xff);
        return data;
    }

}
