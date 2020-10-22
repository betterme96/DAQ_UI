package com.wzb.interfaces;

import java.io.IOException;
import java.net.Socket;

public interface ConfigFactory {
    Config getConfig(Socket commSocket) throws IOException;
}
