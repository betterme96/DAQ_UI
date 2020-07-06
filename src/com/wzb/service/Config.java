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
        while((config = br.readLine()) != null){
            System.out.println(config);
            socketOut.write(config.getBytes());
            Thread.sleep(1000);
        }
        configIn.close();
        br.close();
        System.out.println("Sending config......");
        System.out.println("Sending config suc!");
    }
}
