package com.wzb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileOp {
    public static FileOutputStream createFileOutputStream(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.getParentFile().exists()){
            //System.out.println(fileName + " not exist");
            file.getParentFile().mkdir();
            file.createNewFile();
        }
        return new FileOutputStream(file);
    }
    public static FileInputStream createFileInputStream(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdir();
            file.createNewFile();
        }
        return new FileInputStream(file);
    }
}
