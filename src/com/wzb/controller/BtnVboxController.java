package com.wzb.controller;

import com.wzb.util.RingBuffer;
import com.wzb.util.Time;
import com.wzb.util.WriteLog;
import com.wzb.service.Builder;
import com.wzb.service.Config;
import com.wzb.service.ReadOut;
import com.wzb.service.Store;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.text.ParseException;
import java.util.Timer;

public class BtnVboxController {
    /*
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

     */
    @FXML
    private TextField text_start_time;
    @FXML
    private TextField text_stop_time;
    @FXML
    private TextField text_active_time;
    @FXML
    private Label label_control_state;
    @FXML
    private TextField text_run_number;

    /*
     * DAQ运行状态
     * waiting：1，等待命令输入
     * initialed：2，DAQ系统初始化成功（ringbuffer初始化，模块初始化）
     * configed：3，向电子学发送配置成功
     * running：4，DAQ系统接收到start命令，模块线程开始工作，进行取数
     */
    private int status = 1;

    //各个模块
    private Socket socket;
    private Config config;
    private ReadOut rd;
    private Builder builder;
    private Store store;

    //log文件
    private WriteLog runLogFile;

    //run number文件
    private File runNumFile;
    private int curRunNum;

    //active time控件
    Timer activeTimer;

    public void initEventButton() throws IOException {
        if(status == 1){
            socket = new Socket("172.17.1.165", 8000);

            //init ringbuffer
            RingBuffer[] ringBuffers = new RingBuffer[3];
            int time = 3;
            for(int i = 0; i < ringBuffers.length; ++i){
                ringBuffers[i] = new RingBuffer(1024*100, time);
                time *= 2;
            }

            //init module
            rd = new ReadOut(socket, ringBuffers[0]);
            builder = new Builder(ringBuffers[0], ringBuffers[1]);
            store = new Store(ringBuffers[1]);
            config = new Config(socket);

            //init file
            runLogFile = new WriteLog();
            runNumFile = new File("./daqFile/curRunNumber.txt");

            //get run number
            FileInputStream fileIn = new FileInputStream(runNumFile);
            BufferedReader runNumIn = new BufferedReader(new InputStreamReader(fileIn));
            curRunNum = Integer.parseInt(runNumIn.readLine());//read run number
            fileIn.close();
            runNumIn.close();

            //change DAQ status
            status = 2;
            text_run_number.setText(String.valueOf(curRunNum));
            label_control_state.setText("INITIALIZED");
            System.out.println("init sunc!! ");
        }else{
            System.out.println("illegal op!!");
        }
    }

    public void configEventButton() throws InterruptedException, IOException {
        if(status == 2){
            //send config to electronics
            config.sendEDConfig();
            //change DAQ status
            status = 3 ;
            label_control_state.setText("CONFIGED");String curTime = Time.getCurTime();//get start time
            text_start_time.setText(curTime);//show start time in text field
            text_stop_time.setText("");
            activeTimer = Time.activeTimeShow(text_active_time);

            //create result file and log file
            String runNum = String.format("%04d", curRunNum);//convert to string as file name
            store.setFileName("./daqFile/result/RUN" + runNum + "-RunData.dat");
            runLogFile.createLogFile("./daqFile/log/RUN" + runNum + "-RunSummary.txt");
            runLogFile.writeContent("Start Time:" + curTime + "\n");//write start time to log file

            /*
            //send start command to electronics
            OutputStream out = socket.getOutputStream();
            byte[] comm = new byte[2];
            comm[0] = 0x00;
            comm[1] = 0x01;
            out.write(comm);
            System.out.println("send start");

             */

            //start thread
            new Thread(rd).start();
            // Thread.sleep(1000);
            new Thread(builder).start();
            //Thread.sleep(1000);
            new Thread(store).start();
            //change DAQ status
            status = 4;
            label_control_state.setText("RUNNING");
        }else if(status == 1){
            System.out.println("please init first");
        }else{
            System.out.println("illegal op!!");
        }
    }
    public void startEventButton() throws IOException, InterruptedException {
        if(status == 3){
            String curTime = Time.getCurTime();//get start time
            text_start_time.setText(curTime);//show start time in text field
            text_stop_time.setText("");
            activeTimer = Time.activeTimeShow(text_active_time);

            //create result file and log file
            String runNum = String.format("%04d", curRunNum);//convert to string as file name
            store.setFileName("./daqFile/result/RUN" + runNum + "-RunData.dat");
            runLogFile.createLogFile("./daqFile/log/RUN" + runNum + "-RunSummary.txt");
            runLogFile.writeContent("Start Time:" + curTime + "\n");//write start time to log file

            /*
            //send start command to electronics
            OutputStream out = socket.getOutputStream();
            byte[] comm = new byte[2];
            comm[0] = 0x00;
            comm[1] = 0x01;
            out.write(comm);
            System.out.println("send start");

             */
            
            //start thread
            new Thread(rd).start();
           // Thread.sleep(1000);
            new Thread(builder).start();
            //Thread.sleep(1000);
            new Thread(store).start();
            //change DAQ status
            status = 4;
            label_control_state.setText("RUNNING");

        }else if(status == 1){
            System.out.println("please init ");
        }else if(status == 2){
            System.out.println("please config ");
        }else{
            System.out.println("illegal op");
        }

    }

    public void stopEventButton() throws IOException, ParseException {
        if(status == 4){
            //send stop command to electronics
            OutputStream out = socket.getOutputStream();
            byte[] comm = new byte[2];
            comm[0] = 0x00;
            comm[1] = 0x11;
            out.write(comm);
            System.out.println("send stop!");

            String curTime = Time.getCurTime();//get stop time
            activeTimer.cancel();
            text_stop_time.setText(curTime);//show start time in text field
            runLogFile.writeContent("Stop Time:" + curTime + "\n");//write stop time to log file
            runLogFile.writeContent("good run");
            runLogFile.close();//close the log file

            rd.exit = true;
            builder.exit = true;
            store.exit = true;
            status = 3;

            curRunNum++;

            //write new run number to run number file
            FileOutputStream fileOut = new FileOutputStream(runNumFile);
            BufferedWriter runNumOut = new BufferedWriter(new OutputStreamWriter(fileOut));
            runNumOut.write(String.valueOf(curRunNum));
            runNumOut.close();
            fileOut.close();
            label_control_state.setText("CONFIGED");
        }else{
            System.out.println("please start first!!");
        }
    }
    public void unconfigEventButton(){
        if(status == 3){
            status = 2;
            rd.exit = false;
            builder.exit = false;
            store.exit = false;
            label_control_state.setText("INITIALIZED");
        }else{
            System.out.println("illegal op");
        }
    }
    public void uninitEventButton() throws IOException {
        if(status == 2){
            status = 1;
            socket.close();
            label_control_state.setText("WAITTING");
        }else{
            System.out.println("illegal op");
        }
    }

}
