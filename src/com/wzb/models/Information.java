package com.wzb.models;

import javafx.beans.property.SimpleStringProperty;

public class Information {
    private final SimpleStringProperty time;
    private final SimpleStringProperty type;
    private final SimpleStringProperty info;


    public Information(String time, String type, String info){
        this.time = new SimpleStringProperty(time);
        this.type = new SimpleStringProperty(type);
        this.info = new SimpleStringProperty(info);
    }

    public String getTime() {
        return time.get();
    }

    public void setTime(String time) {
        this.time.set(time);
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public String getInfo() {
        return info.get();
    }

    public void setInfo(String info) {
        this.info.set(info);
    }
}
