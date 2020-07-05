package com.wzb.service;


import java.net.Socket;

public class Config {
    private Socket commSocket;

    public Config(Socket commSocket){
        this.commSocket = commSocket;
    }

    public void sendConfig() throws InterruptedException {
        System.out.println("Sending config......");
        Thread.sleep(3000);
        System.out.println("Sending config suc!");
    }
}
