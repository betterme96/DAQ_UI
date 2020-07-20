package com.wzb.util;

public class StringOp {
    public static byte[] string2hex(String s) {
        //System.out.println("to byte string:" + s);
        byte[] data = new byte[8];
        for(int i = 0; i < 8; ++i){
            int num = Integer.parseInt(s.substring(i*2,i*2+2),16);
            data[i] = (byte) (num & 0xff);
        }
        return data;
    }

    public static String byte2string(byte[] config) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < config.length; i++) {
            String hex = Integer.toHexString(config[i] & 0xFF);
            if(hex.length() < 2) {
                hex = "0" + hex;
            }
            sb.append(hex.toUpperCase());
        }

        return sb.toString();
    }
}
