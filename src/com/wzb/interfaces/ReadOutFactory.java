package com.wzb.interfaces;

import com.wzb.util.RingBuffer;

import java.io.IOException;
import java.net.Socket;

public interface ReadOutFactory {
    ReadOut getReadOut(Socket dataSocket, RingBuffer ringBuffer) throws IOException;
}
