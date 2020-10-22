package com.wzb.interfaces;

import java.io.IOException;
import java.util.List;

public interface Config {
    void work(List<String> config) throws IOException, InterruptedException;
    void sendStart() throws IOException;
}
