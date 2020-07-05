package com.wzb.controller;

import com.wzb.helper.RingBuffer;
import com.wzb.helper.Time;
import com.wzb.helper.WriteLog;
import com.wzb.models.Status;
import com.wzb.service.Builder;
import com.wzb.service.Config;
import com.wzb.service.ReadOut;
import com.wzb.service.Store;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BtnVboxController {
    @FXML
    private Button btn_init;
    @FXML
    private Button btn_config;
    @FXML
    private Button btn_start;
    @FXML
    private Button btn_stop;
    @FXML
    private Button btn_unconfig;
    @FXML
    private Button btn_uninit;
    @FXML
    private TextField text_start_time;
    @FXML
    private TextField text_stop_time;


    private Status status;
    private Socket commSocket;
    private Config config;
    private ReadOut rd;
    private Builder builder;
    private Store store;

    private static int runNum = 1;

    public WriteLog runLog = new WriteLog();

    public void setStatus(Status status) {
        this.status = status;
    }

    public void initEventButton() throws IOException {
        if(status.getStatus() == 1){
            commSocket = new Socket("127.0.0.1", 8000);

            //init ringbuffer
            RingBuffer[] ringBuffers = new RingBuffer[3];
            int time = 3;
            for(int i = 0; i < ringBuffers.length; ++i){
                ringBuffers[i] = new RingBuffer(1024*3, time);
                time *= 2;
            }

            //init module
            store = new Store(ringBuffers[1]);
            builder = new Builder(ringBuffers[0], ringBuffers[1], store);
            rd = new ReadOut("127.0.0.1", "8001", ringBuffers[0], builder);
            config = new Config(commSocket);

            //start thread
            new Thread(rd).start();
            new Thread(builder).start();
            new Thread(store).start();
            status.setStatus(2);
            System.out.println("init sunc!! ");
        }else{
            System.out.println("illegal op!!");
        }
    }

    public void configEventButton() throws InterruptedException {
        if(status.getStatus() == 2){
            config.sendConfig();
            status.setStatus(3);
            System.out.println("config sunc!! ");
        }else if(status.getStatus() == 1){
            System.out.println("please init first");
        }else{
            System.out.println("illegal op!!");
        }
    }
    public void startEventButton() throws IOException, InterruptedException {
        int curStatus = status.getStatus();

        if(curStatus == 3){
            store.fileName = Time.getCurDate() + runNum;
            runLog.createLogFile("./log/RUN" + Time.getCurDate() + runNum + "-RunSummary.txt");

            String curTime = Time.getCurTime();
            runLog.writeContent("Start Time:" + curTime + "\n");
            text_start_time.setText(curTime);

            OutputStream out = commSocket.getOutputStream();
            byte[] comm = new byte[2];
            comm[0] = 0x00;
            comm[1] = 0x01;
            out.write(comm);


            rd.start = true;
            Thread.sleep(1000);
            builder.start = true;
            Thread.sleep(1000);
            store.start = true;
            status.setStatus(4);
        }else if(curStatus == 1){
            System.out.println("please init ");
        }else if(curStatus == 2){
            System.out.println("please config ");
        }else{
            System.out.println("illegal op");
        }
    }
    public void stopEventButton() throws IOException {
        int curStatus = status.getStatus();
        if(curStatus == 4){
            OutputStream out = commSocket.getOutputStream();
            byte[] comm = new byte[2];
            comm[0] = 0x00;
            comm[1] = 0x11;
            out.write(comm);

            String curTime = Time.getCurTime();
            runLog.writeContent("Stop Time:" + curTime + "\n");
            text_stop_time.setText(curTime);

            runLog.close();
            System.out.println("send stop!");
            status.setStatus(3);
            runNum++;
        }else{
            System.out.println("please start first!!");
        }
    }
    public void unconfigEventButton(){
        if(status.getStatus() == 3){
            status.setStatus(2);
        }else{
            System.out.println("illegal op");
        }
    }
    public void uninitEventButton() throws IOException {
        if(status.getStatus() == 2){
            status.setStatus(1);
            rd.exit = true;
            builder.exit = true;
            store.exit = true;
            commSocket.close();
        }else{
            System.out.println("illegal op");
        }
    }

}
