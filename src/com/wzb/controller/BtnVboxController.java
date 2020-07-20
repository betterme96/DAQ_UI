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
import java.net.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    //active time控件
    Timer activeTimer;
    /*
     * DAQ运行状态
     * waiting：1，等待命令输入
     * initialed：2，DAQ系统初始化成功（ringbuffer初始化，模块初始化）
     * configed：3，向电子学发送配置成功
     * running：4，DAQ系统接收到start命令，模块线程开始工作，进行取数
     */
    private int status = 1;

    private Socket dataSocket;//用于接收数据的tcp socket
    private DatagramSocket commSocket;//用于发送配置的udp socket
    InetSocketAddress peerAddr;//用于发送udp数据包的目标地址

    //各个模块
    private Config config;
    private ReadOut rd;
    private Builder builder;
    private Store store;

    private WriteLog runLogFile;//保存run信息的log文件

    private File runNumFile;//保存run number的文件
    private int curRunNum;//当前可用run number

    private List<String> configs;

    private static final String DEST_IP = "192.168.0.10";//电子学ip
    private static final int TCP_DEST_PORT = 8000;//电子学发送数据端口
    private static final int UDP_DEST_PORT = 8001;//电子学接收数据端口
    private static final int SRC_PORT = 8000;//本机接收数据端口


    public void initEventButton() throws IOException {
        if(status == 1){
            try {
                //初始化用于数据接收的socket
                dataSocket = new Socket(DEST_IP, TCP_DEST_PORT);
                //初始化用于发送配置和接收回包的socket
                commSocket = new DatagramSocket(SRC_PORT);
                //初始化用于发送配置的电子学socket地址
                peerAddr = new InetSocketAddress(DEST_IP, UDP_DEST_PORT);

            }catch (SocketException e){
                if(dataSocket == null){
                    System.out.println("tcp connect fail");
                }
            }
            //初始化configs容器
            configs = new ArrayList<>();
            //初始化ringbuffer
            RingBuffer[] ringBuffers = new RingBuffer[3];
            int time = 2;
            for(int i = 0; i < ringBuffers.length; ++i){
                ringBuffers[i] = new RingBuffer(1024*100, time);
                time *= 2;
            }

            //初始化各个模块
            rd = new ReadOut(dataSocket, ringBuffers[0]);
            builder = new Builder(ringBuffers[0], ringBuffers[1]);
            store = new Store(ringBuffers[1]);
            //config = new Config(dataSocket);
            config = new Config(commSocket, peerAddr);

            //初始化log文件
            runLogFile = new WriteLog();

            //获取当前可用run number
            runNumFile = new File("./daqFile/curRunNumber.txt");
            FileInputStream fileIn = new FileInputStream(runNumFile);
            BufferedReader runNumIn = new BufferedReader(new InputStreamReader(fileIn));
            curRunNum = Integer.parseInt(runNumIn.readLine());//read run number
            fileIn.close();
            runNumIn.close();

            //change DAQ status
            status = 2;
            label_control_state.setText("INITIALIZED");
            System.out.println("init sunc!! ");
        }else{
            System.out.println("illegal op!!");
        }
    }

    public void configEventButton() throws InterruptedException, IOException {
        if(status == 2){
            //send config to electronics
            //config.sendEDConfig();

            //获取配置指令
            FileInputStream configIn = new FileInputStream(new File("./daqFile/config/QXAFS-config.txt"));
            BufferedReader br = new BufferedReader(new InputStreamReader(configIn));
            String curLine = "first";
            while ((curLine = br.readLine()) != null){
                //System.out.println(curLine);
                configs.add(curLine);
            }
            configIn.close();
            br.close();
            //发送配置，通道1和通道2有效
            config.sendQXAFSConfig(configs, 3);


            //change DAQ status
            status = 3 ;
            label_control_state.setText("CONFIGED");

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
            text_run_number.setText(String.valueOf(curRunNum));

            //create result file and log file
            String runNum = String.format("%04d", curRunNum);//convert to string as file name
            store.setFileName("./daqFile/result/RUN" + runNum + "-RunData.dat");
            runLogFile.createLogFile("./daqFile/log/RUN" + runNum + "-RunSummary.txt");
            runLogFile.writeContent("Start Time:" + curTime + "\n");//write start time to log file

            //发送start命令
            config.sendQXAFSStart();


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
            OutputStream out = dataSocket.getOutputStream();
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
            dataSocket.close();
            label_control_state.setText("WAITTING");
        }else{
            System.out.println("illegal op");
        }
    }

}
