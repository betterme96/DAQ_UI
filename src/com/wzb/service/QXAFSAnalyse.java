package com.wzb.service;

import com.wzb.util.FileOp;
import com.wzb.util.RingBuffer;
import com.wzb.util.StringOp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class QXAFSAnalyse implements Runnable {
    private RingBuffer preBuffer;
    private RingBuffer nextBuffer;
    private String fileName;
    private PrintWriter computeDataFile;

    private int fileNo = 1;
    private int freq = 0;//抽样频率
    private int count = 0;//数据压缩比值
    private int start = 0;
    private int end = 0;
    private int chCount = 0;

    private List<List<Integer>> adcList = new ArrayList<>();//存放四路ADC数据
    private List<double[]> nodes;

    public volatile  boolean exit = false;

    public QXAFSAnalyse(RingBuffer preBuffer, RingBuffer nextBuffer){
        this.preBuffer = preBuffer;
        this.nextBuffer = nextBuffer;
     }

    public void setParam(int start, int end, int count, int freq, int chCount, List<double[]> nodes){
        this.start = start;
        this.end = end;
        this.count = count;
        this.freq = freq;
        this.chCount = chCount;
        this.nodes = nodes;
        for(int i = 0; i < chCount; ++i){
            adcList.add(new ArrayList<>());
        }
    }

    public void setComputeDataFile(String fileName) throws IOException {
        this.fileName = fileName;
        this.computeDataFile = new PrintWriter(FileOp.createFileOutputStream(fileName+"_"+fileNo + ".txt"));
    }

    @Override
    public void run() {
        System.out.println("Analyse module working......");
        check();
        //uncheck();
    }

    private void uncheck() {
        byte[] data = new byte[5000000];
        try{
            while (preBuffer.read(data, 0, data.length, "Analyse Module") != -1){
                nextBuffer.write(data, 0, data.length, "Analyse Module");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void check() {
            int predirect = 0;
            int direct = 0;
            int pulse = 0;
            int channel = 0;
            int adc = 0;

            byte[] curData = new byte[4];//用于从preBuffer中读取数据
            byte[] nextData = null;
            StringBuilder computData = new StringBuilder();//存储解析数据
            StringBuilder analyseData = new StringBuilder();

            int pointCount = 0;//读取的数据个数
            double avg1 = 0, avg2 = 0, res = 0;
            int time = 0;

            try {
                computeDataFile.println("pulse adc1 adc2 absorption");
                while(preBuffer.read(curData, 0, 4, "Analyse Module") !=-1){
                    //System.out.println(StringOp.byte2string(curData));
                    //清空用于写入文件的字符串
                    computData.delete(0,computData.length());
                    //清空用于写入下一级buffer的字符串
                    analyseData.delete(0, analyseData.length());

                    if(curData[0] == (byte) 0xff){
                        if(!isEmpty(adcList)){
                            //目前只有通道1和通道2有数据，并且只有如何计算这两个通道的算法
                            avg1 = getAvg(adcList.get(0));
                            avg2 = getAvg(adcList.get(1));
                            res = Math.log(avg1/avg2);
                            //res = avg1;
                            computData.append(pulse + " " + avg1 + " " + avg2 + " " + res);
                            computeDataFile.println(computData);
                            if(pointCount == 0 || pointCount % freq == 0){
                                nodes.add(new double[]{time, res});
                                time++;
                            }
                            clear(adcList);
                            pointCount++;
                        }
                        pulse = (curData[1] << 16 & 0xffffff) | (curData[2] << 8 & 0xffff) | (curData[3] & 0xff);
                    }else{

                        direct = ((curData[0] >> 4) & 0xf) - 13;

                        if(predirect != 0 && predirect != direct){
                            //System.out.println("pre:" + predirect + "  cur:" + direct);
                            //当前数据写入
                            if(!isEmpty(adcList)){
                                avg1 = getAvg(adcList.get(0));
                                avg2 = getAvg(adcList.get(1));
                                res = Math.log(avg1/avg2);
                                //res = avg1;
                                computData.append(pulse + " " + avg1 + " " + avg2 + " " + res);
                                computeDataFile.println(computData);
                                clear(adcList);
                            }
                            computeDataFile.close();
                            fileNo++;
                            computeDataFile = new PrintWriter(FileOp.createFileOutputStream(fileName+"_"+fileNo + ".txt"));
                            computeDataFile.println("pulse adc1 adc2 absorption");

                        }

                        predirect = direct;

                        channel = curData[0] & 0xf;

                        adc = ((curData[1] & 0xff) << 16) | ((curData[2] & 0xff) << 8) | (curData[3] & 0xff);
                        //System.out.println(channel);
                        adcList.get(channel-3).add(adc);

                        if(channel == 1){//可替换成选中的通道中的最小通道号
                            analyseData.append(pulse + " "+ adc + " ");
                        }else {
                            analyseData.append(adc + "\n");
                        }
                    }

                    nextData = analyseData.toString().getBytes();
                    nextBuffer.write(nextData, 0, nextData.length, "Analyse Module");
                }
                if(!isEmpty(adcList)){
                    //目前只有通道1和通道2有数据，并且只有如何计算这两个通道的算法
                    avg1 = getAvg(adcList.get(0));
                    avg2 = getAvg(adcList.get(1));
                    res = Math.log(avg1/avg2);
                    //res = avg1;
                    computData.append(pulse + " " + avg1 + " " + avg2 + " " + res);
                    computeDataFile.println(computData);
                    if(pointCount == 0 || pointCount % freq == 0){
                        nodes.add(new double[]{time, res});
                        time++;
                    }
                    pointCount++;
                    clear(adcList);
                }
                System.out.println("count:" + pointCount);
                computeDataFile.flush();
                computeDataFile.close();
                this.exit = true;

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void clear(List<List<Integer>> adcList) {
        for(int i = 0; i < adcList.size(); ++i){
            adcList.get(i).clear();
        }
    }

    private double getAvg(List<Integer> adcList) {
        int len = (adcList.size() - start - end) * count;
        int sum = 0;
        for(int i = start; i < adcList.size()-end; ++i){
            sum += adcList.get(i);
        }
        return (double)(sum/len);
    }

    private boolean isEmpty(List<List<Integer>> adcList) {
        for(int i = 0; i < adcList.size(); ++i){
            if(adcList.get(i).size()==0){
                return true;
            }
        }
        return false;
    }

}
