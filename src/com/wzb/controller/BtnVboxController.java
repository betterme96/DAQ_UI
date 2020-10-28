package com.wzb.controller;

import com.wzb.models.Information;
import com.wzb.service.*;
import com.wzb.util.RingBuffer;
import com.wzb.util.Time;
import com.wzb.util.WriteLog;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BtnVboxController  implements Initializable {
    @FXML
    private GridPane gridPane;
    /*
     * 界面相关控件
     */
    @FXML
    private TextField text_start_time;//发送start命令时间
    @FXML
    private TextField text_stop_time;//发送stop命令时间
    @FXML
    private TextField text_active_time;//进行取数的时间
    @FXML
    private TextField text_run_number;//当前可用run number
    @FXML
    private  TextField text_ip;//前端的IP地址
    @FXML
    private TextField text_port;//前端的端口号
    @FXML
    private TextField text_start;//前无效数据量
    @FXML
    private TextField text_end;//后无效数据量
    @FXML
    private TextField text_count;//累加量
    @FXML
    private TextField text_freq;//采样频率

    @FXML
    private TextField configFilePath;
    @FXML
    private TextField destFolderPath;
    @FXML
    private Label label_control_state;
    @FXML
    private Button btn_connect;
    @FXML
    private Button btn_config;
    @FXML
    private Button btn_start;
    @FXML
    private Button btn_stop;
    @FXML
    private Button btn_disconn;
    @FXML
    private RadioButton ch1;
    @FXML
    private RadioButton ch2;
    @FXML
    private RadioButton ch3;
    @FXML
    private RadioButton ch4;

    Timer activeTimer;//计时器控件

    @FXML
    private TabPane tabPane;
    //linechar相关
    @FXML
    private AnchorPane wavePane;
    private LineChart lineChart;
    private ObservableList<XYChart.Series<Number,Number>> obSeries = FXCollections.observableArrayList();
    private XYChart.Series<Number,Number> xySeries = new XYChart.Series<>();//向其中存放数据
    private NumberAxis xAxis;//x轴
    private NumberAxis yAxis;//y轴
    private ScheduledExecutorService scheduledExecutorService;//用于定时刷新wave
    private List<double[]> nodes = new ArrayList<>();//用于收集需要加入wave的点
    private int start = 0;//从nodes的start处开始向series加点

    //tableView相关
    @FXML
    private Pane tablePane;
    private TableView infoTable;
    private ObservableList<Information> tableData = FXCollections.observableArrayList();



    /*
     * 与数据流部分相关的对象
     */
    private Socket dataSocket;//用于接收数据的socket
    private Socket commSocket;//用于发送配置的socket

    private File configFile = null;//配置文件初始为null，必须选定配置文件
    private WriteLog runLogFile;//保存run信息的log文件

    private File runNumFile;//保存run number的文件
    private int curRunNum;//当前可用run number

    private List<String> configList;//用于保存从文件中读出的配置命令+通道使能
    private int channel;//通道使能值
    private int chChoose1 = 0, chChoose2 = 0;
    private RingBuffer[] ringBuffers;
    private Thread[] threads;
    private int[] channels;

    //各个模块的实例化对象
    private QXAFSConfig config;
    private QXAFSReadOut readout;
    private QXAFSAnalyse analyse;
    private QXAFSStore store;

    /*
     * 配置界面的默认值
     */
    private String DEST_IP = "192.168.0.10";//电子学ip
    private int DEST_PORT = 8000;//电子学发送数据端口
    private int START = 0;
    private int END = 0;
    private int COUNT = 10;
    private int FREQ = 100;


    private int BUFF_SIZE = 20*64*1000000;//ringbuffer大小
    private int BUFF_OUT_TIME = 11;//ringbuffer读写超时时间
    private int SOCKET_OUT_TIME = 10000;//socket超时时间
    private String runNumFileName = "./daqFile/curRunNumber.txt";//保存runNumber的文件

    private int i = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //对配置界面进行默认值的填充
        initTextField();

        //对linechart初始化
        initLineChart();

        //对tableView初始化
        initTableView();


        //对按钮进行初始化
        initButton();

        //对ringbuffer进行初始化，并设置超时时间
        ringBuffers = new RingBuffer[3];
        for(int i = 0; i < ringBuffers.length; ++i){
            ringBuffers[i] = new RingBuffer(BUFF_SIZE, BUFF_OUT_TIME+i);
        }

        //对保存配置命令的容器进行初始化
        configList = new ArrayList<>();

        //对线程数组进行初始化
        threads = new Thread[3];
        //对通道数组进行初始化
        channels = new int[4];

        //修改DAQ状态
        label_control_state.setText("INITIALIZED");
        tableData.add(new Information(Time.getCurTime(), "INFO","软件初始化成功"));
    }

    private void initTableView() {
        infoTable = new TableView();

        TableColumn timeCol = new TableColumn("Time");
        TableColumn typeCol = new TableColumn("Type");
        TableColumn infoCol = new TableColumn("Info");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.prefWidthProperty().bind(infoTable.widthProperty().multiply(0.2));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.prefWidthProperty().bind(infoTable.widthProperty().multiply(0.1));
        infoCol.setCellValueFactory(new PropertyValueFactory<>("info"));
        infoCol.prefWidthProperty().bind(infoTable.widthProperty().multiply(0.7));

        infoTable.getColumns().addAll(timeCol, typeCol, infoCol);
        infoTable.setItems(tableData);
        infoTable.setPlaceholder(new Label(""));

        tablePane.getChildren().add(infoTable);
        infoTable.prefWidthProperty().bind(tablePane.widthProperty());
        infoTable.prefHeightProperty().bind(tablePane.heightProperty());


        /*
        typeCol.setCellFactory(new Callback<TableColumn, TableCell>() {
            @Override
            public TableCell call(TableColumn param) {
                return new TableCell<Information,String>(){
                    ObservableValue<String> ov;
                    @Override
                    public void updateItem(String item, boolean empty){
                        super.updateItem(item, empty);
                        if(!empty){
                            ov = getTableColumn().getCellObservableValue(getIndex());
                            if(getTableRow() != null && item.contains("ERROR")){
                                this.getTableRow().setStyle("-fx-background-color: #ff0000");
                            }
                            setText(item);
                        }
                    }
                };
            }
        });

         */

    }

    private void initButton() {
        btn_connect.setDisable(false);
        btn_config.setDisable(true);
        btn_start.setDisable(true);
        btn_stop.setDisable(true);
        btn_disconn.setDisable(true);
    }

    private void initTextField() {
        text_ip.setText(DEST_IP);
        text_port.setText(String.valueOf(DEST_PORT));
        text_freq.setText(String.valueOf(FREQ));
        text_start.setText(String.valueOf(START));
        text_end.setText(String.valueOf(END));
        text_count.setText(String.valueOf(COUNT));
    }

    private void initLineChart() {
        xAxis = new NumberAxis();
        xAxis.setLabel("time");

        yAxis = new NumberAxis();
        yAxis.setLabel("μx(E)");

        lineChart = new LineChart(xAxis, yAxis);
        obSeries.add(xySeries);
        lineChart.setData(obSeries);
        lineChart.setCreateSymbols(false);
        lineChart.prefWidthProperty().bind(wavePane.widthProperty().subtract(10));
        lineChart.prefHeightProperty().bind(wavePane.heightProperty().subtract(10));
        wavePane.getChildren().add(lineChart);

        wavePane.prefWidthProperty().bind(tabPane.widthProperty());
        wavePane.prefHeightProperty().bind(tabPane.heightProperty());
        tabPane.prefWidthProperty().bind(gridPane.widthProperty());
        tabPane.prefHeightProperty().bind(gridPane.heightProperty());
    }


    public void connectButtonEvent(){
        //初始化用于数据接收的socket
        if(text_ip.getText().length() == 0 || text_port.getText().length() == 0){
            showDialog("请输入正确的IP地址和端口！");
            return;
        }
        try{
            //获取界面上输入的
            DEST_IP = text_ip.getText();
            DEST_PORT = Integer.valueOf(text_port.getText());


            //进行TCP连接
            dataSocket = new Socket(DEST_IP, DEST_PORT);//用于数据接收的socket
            commSocket = dataSocket;//用于配置发送的socket
            dataSocket.setSoTimeout(SOCKET_OUT_TIME);//设置超时时间为10秒
            tableData.add(new Information(Time.getCurTime(), "INFO", "TCP connect successful"));

            //tcp连接后，可以进行配置或者断开连接操作
            btn_connect.setDisable(true);
            btn_config.setDisable(false);
            btn_disconn.setDisable(false);
        } catch (SocketException e) {
            // e.printStackTrace();
            tableData.add(new Information(Time.getCurTime(), "ERROR", "TCP connect fail,  reason:" + e.getMessage()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            tableData.add(new Information(Time.getCurTime(), "ERROR", "TCP connect fail,  reason:" + e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
            tableData.add(new Information(Time.getCurTime(), "ERROR", "TCP connect fail,  reason:" + e.getMessage()));
        }


    }

    /*
     * 配置
     * 1.接收电子学发送的数据
     * 2.从配置文件逐条读取指令到config_list
     * 3.获取使能通道,发送通道使能命令
     */
    public void configButtonEvent() throws InterruptedException, IOException {
        /*
        new Thread(()->{

            Thread t1 = new Thread(()->{
                try{
                    while (i < 10){
                        System.out.println(i);
                        i++;
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            Thread t2 = new Thread(()->{
                showDialog(String.valueOf(i));
                //System.out.println(i);
            });
            t1.start();
            try {
                t1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(t2);
        }).start();

         */

        //如果没有指定的配置文件
        if(configFile == null){
            showDialog("请选择配置文件");
            return;
        }

        //获取通道使能值
        channels[0] = (ch1.isSelected() ? 1 : 0);
        channels[1] = (ch2.isSelected() ? 1 : 0);
        channels[2] = (ch3.isSelected() ? 1 : 0);
        channels[3] = (ch4.isSelected() ? 1 : 0);
        int count = 0;
        for(int ch : channels){
            count += ch;
        }

        if(count != 2){
            showDialog("请选择两个通道！");
            return;
        }
        channel = channels[3]<< 3;
        channel +=channels[2] << 2;
        channel +=channels[1] << 1;
        channel +=channels[0];

        //对DAQ系统进行相关配置
        START = Integer.parseInt(text_start.getText());
        END = Integer.parseInt(text_end.getText());
        COUNT = Integer.parseInt(text_count.getText());
        FREQ = Integer.parseInt(text_freq.getText());

        text_start.setEditable(false);
        text_end.setEditable(false);
        text_count.setEditable(false);
        text_freq.setEditable(false);

        //获取选择的两个通道
        for(int i = 0; i < channels.length; ++i){
            if(channels[i] == 1){
                chChoose1 = i;
                break;
            }
        }

        for(int i = channels.length-1; i > -1; --i){
            if(channels[i] == 1){
                chChoose2 = i;
                break;
            }
        }

        /*
        System.out.println("channel:" + channel);
        System.out.println("ch1:" + ch1);
        System.out.println("ch2:" + ch2);

         */



        //初始化config模块
        config = new QXAFSConfig(commSocket);

        //读取文件获取配置指令
        FileInputStream configIn =  new FileInputStream(configFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(configIn));
        String curLine = null;
        while ((curLine = br.readLine()) != null){
            //System.out.println(curLine);
            if(curLine.length()>0){
                configList.add(curLine);
            }
        }
        configList.add(String.valueOf(channel));

        /*
        System.out.println(configList.size());
        for(String config : configList){
            System.out.println(config);
        }

         */

        configIn.close();
        br.close();

        //发送配置，使能通道
        config.work(configList);

        //change DAQ status
        label_control_state.setText("CONFIGED");
        tableData.add(new Information(Time.getCurTime(), "INFO", "config successful"));
        btn_config.setDisable(true);
        btn_start.setDisable(false);
    }


    /*
     * start
     * 1. 更新start time
     * 2. 启动active time计时
     * 3. 发送start指令
     * 4. 启动各个模块线程
     * 5. 修改状态
     */
    public void startButtonEvent() throws IOException, InterruptedException {

        //如果没有选定保存文件的文件夹
        if(destFolderPath.getText().length() == 0){
            showDialog("请选择保存文件夹");
            return;
        }

        Thread.sleep(3000);
        String curTime = Time.getCurTime();//获取开始时间

        //更新start time，active time
        Platform.runLater(()->{
            text_start_time.setText(curTime);//show start time in text field
            text_stop_time.setText("");
            activeTimer = Time.activeTimeShow(text_active_time);
            text_run_number.setText(String.valueOf(curRunNum));
        });


        //获取当前可用run number，然后将run number+1写回文件
        runNumFile = new File(runNumFileName);
        BufferedReader runNumIn = new BufferedReader(new InputStreamReader(new FileInputStream(runNumFile)));
        curRunNum = Integer.parseInt(runNumIn.readLine());//read run number
        runNumIn.close();
        BufferedWriter runNumOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(runNumFile)));
        runNumOut.write(String.valueOf(curRunNum+1));
        runNumOut.close();


        //初始化各个模块
        readout = new QXAFSReadOut(dataSocket, ringBuffers[0]);
        analyse = new QXAFSAnalyse(ringBuffers[0], ringBuffers[1]);
        store = new QXAFSStore(ringBuffers[1]);

        threads[0] = new Thread(readout);
        threads[1] = new Thread(analyse);
        threads[2] = new Thread(store);

        //获取保存结果文件的文件名
        //destFolderPath是已经存在的文件夹，destFolderPath+curRunName是不存在的文件夹， destFolderPath+curRunName+curRunName是一个还没有加后缀的不存在的文件
        String curRunName = "/RUN" + String.format("%04d", curRunNum) + "-RunData";
        String curRunLogName = "/RUN" + String.format("%04d", curRunNum) + "-RunSummary.txt";
        String destFilePath = destFolderPath.getText() + curRunName + curRunName;
        //System.out.println(destFilePath);


        //设置各个模块保存数据的文件
        readout.setRawDataFile(destFilePath);
        analyse.setComputeDataFile(destFilePath);
        store.setAnalyseDataFile(destFilePath);

        //初始化log文件
        runLogFile = new WriteLog();
        runLogFile.createLogFile(destFolderPath.getText() + curRunName + curRunLogName);
        runLogFile.writeContent("Config File:" + configFile.getName() + "\n");
        runLogFile.writeContent("Start Time:" + curTime + "\n");//write start time to log file

        //模块参数配置
        readout.setLogFile(runLogFile);
        readout.setTableData(tableData);

        //System.out.println("channel choose:" + chChoose1 + "  " + chChoose2);
        analyse.setParam(START, END, COUNT, FREQ, chChoose1, chChoose2, nodes);
        analyse.setLogFile(runLogFile);
        analyse.setTableData(tableData);

        store.setLogFile(runLogFile);
        store.setTableData(tableData);

        scheduledExecutorService  = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(()->{
            Platform.runLater(()->{
                int size = nodes.size();
                //System.out.println("nodes size:" + size);
                for(int i = start; i < size; ++i){
                    xySeries.getData().add(new XYChart.Data<>(nodes.get(i)[0], nodes.get(i)[1]));
                }
                start = size;
            });
        },0,1000, TimeUnit.MILLISECONDS);


        //发送start命令
        config.sendStart();

        for(int i = 0; i < 3; ++i){
            threads[i].start();
        }

        label_control_state.setText("RUNNING");
        tableData.add(new Information(Time.getCurTime(), "INFO", "Start to work"));
        btn_start.setDisable(true);
        btn_stop.setDisable(false);
        btn_disconn.setDisable(true);//start之后不能通过断开连接的方式停止工作
    }


    /*
     * stop
     * 1. 关闭active time计时
     * 2. 更新stop time
     * 3. 关闭工作线程
     * 4. 修改程序状态
     */
    public void stopButtonEvent() throws IOException, ParseException {
        String curTime = Time.getCurTime();//get stop time

        //System.out.println(scheduledExecutorService.isShutdown());
        Platform.runLater(()->{
            activeTimer.cancel();
            text_stop_time.setText(curTime);//show start time in text field

            //关闭画图线程
            scheduledExecutorService.shutdownNow();//关闭画图线程

            //关闭工作线程
            /*
            for(int i = 0; i < 3; ++i){
                threads[i].interrupt();
            }

             */
            label_control_state.setText("STOP");
            tableData.add(new Information(Time.getCurTime(), "INFO", "stop work"));
            btn_stop.setDisable(true);

            readout.isExit = true;
        });

        //关闭logFile
        new Thread(()->{
            try {
                while (!store.over){
                    Thread.sleep(1000);
                }
                runLogFile.writeContent("Stop Time:" + curTime + "\n");//write stop time to log file
                if(analyse.isError){
                    runLogFile.writeContent("bad run\n");
                }
                runLogFile.writeContent("good run\n");
                runLogFile.close();

                btn_config.setDisable(false);
                btn_start.setDisable(false);
                btn_disconn.setDisable(false);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void disconnButtonEvent() throws IOException {
        //断开tcp连接，必须重连
        dataSocket.close();
        btn_connect.setDisable(false);
        btn_config.setDisable(true);
        btn_start.setDisable(true);
        btn_stop.setDisable(true);
    }

    public void configFileAdd_Action(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("配置文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT", "*.txt"));
        File file = fileChooser.showOpenDialog(gridPane.getScene().getWindow());
        if(file == null && configFile != null){
            return;
        }
        configFile = file;
        configFilePath.setText(configFile.getName());
        configFilePath.setTooltip(new Tooltip(configFile.getPath()));
    }

    public void destFilePath_Action(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("保存到");
        destFolderPath.setText(directoryChooser.showDialog(gridPane.getScene().getWindow()).getPath());
        destFolderPath.setTooltip(new Tooltip(destFolderPath.getText()));
    }


    private static void showDialog(String info) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(info);

        alert.showAndWait();
    }
}
