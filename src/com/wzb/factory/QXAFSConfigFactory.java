package com.wzb.factory;

import com.wzb.interfaces.Config;
import com.wzb.interfaces.ConfigFactory;
import com.wzb.service.QXAFSConfig;

import java.io.IOException;
import java.net.Socket;

public class QXAFSConfigFactory  implements ConfigFactory {
    @Override
    public Config getConfig(Socket commSocket) throws IOException {
        return new QXAFSConfig(commSocket);
    }
}
