package com.wzb.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteLog {
    private FileOutputStream logFile;

    public int createLogFile(String fileName) throws IOException {
        try{
            File file = new File(fileName);
            if(!file.exists()){
                file.createNewFile();
            }
            this.logFile = new FileOutputStream(file);
            return 1;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    public void writeContent(String str) throws IOException {
        logFile.write(str.getBytes());
    }

    public void close() throws IOException {
        logFile.close();
    }

}
