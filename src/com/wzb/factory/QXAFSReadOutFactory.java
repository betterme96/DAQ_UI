package com.wzb.factory;

import com.wzb.interfaces.ReadOut;
import com.wzb.interfaces.ReadOutFactory;
import com.wzb.service.QXAFSReadOut;
import com.wzb.util.RingBuffer;

import java.io.IOException;
import java.net.Socket;

public class QXAFSReadOutFactory{

}
/*
public class QXAFSReadOutFactory implements ReadOutFactory {
    @Override
    public ReadOut getReadOut(Socket dataSocket, RingBuffer ringBuffer) throws IOException {
        return new QXAFSReadOut(dataSocket, ringBuffer);
    }
}

 */
