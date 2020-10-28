package com.wzb.service;

import com.wzb.interfaces.Store;
import com.wzb.models.Information;
import com.wzb.util.FileOp;
import com.wzb.util.RingBuffer;
import com.wzb.util.Time;
import com.wzb.util.WriteLog;
import javafx.collections.ObservableList;


import java.io.FileOutputStream;
import java.io.IOException;

public class QXAFSStore implements Runnable {
    private RingBuffer ringBuffer;
    private FileOutputStream analyseDataFile;//保存解析的数据文件
    private WriteLog runLogFile;//保存run信息的log文件
    private ObservableList<Information> tableData;//tableView信息填充

    public volatile boolean over = false;
    public QXAFSStore(RingBuffer ringBuffer){
        this.ringBuffer = ringBuffer;
    }

    public void setAnalyseDataFile(String fileName) throws IOException {
        this.analyseDataFile = FileOp.createFileOutputStream(fileName + ".txt");
    }

    public void setLogFile(WriteLog runLogFile){
        this.runLogFile = runLogFile;
    }

    public void setTableData(ObservableList<Information> tableData){
        this.tableData = tableData;
    }

    @Override
    public void run(){
        try{
            runLogFile.writeContent(Time.getCurTime() + ":Store module start work\n");
            //tableData.add(new Information(Time.getCurTime(), "INFO", "Store module working....." ));
            System.out.println("Store module working......");

            byte[] data = new byte[5000000];
            int len = 0;
            analyseDataFile.write("pulse adc1 adc2\n".getBytes());
            while ((len = ringBuffer.read(data, 0, data.length, "Store Module") )!= -1){
                analyseDataFile.write(data, 0, len);
            }
            analyseDataFile.flush();
            analyseDataFile.close();
            runLogFile.writeContent(Time.getCurTime());
            runLogFile.writeContent(":Store module stop work\n");
            over = true;
            tableData.add(new Information(Time.getCurTime(), "INFO", "Store data success" ));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
