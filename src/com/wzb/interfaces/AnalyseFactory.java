package com.wzb.interfaces;

import com.wzb.service.QXAFSAnalyse;
import com.wzb.util.RingBuffer;

import java.io.IOException;

public interface AnalyseFactory {
    Analyse getAnalyse(RingBuffer preBuffer, RingBuffer nextBuffer) throws IOException;
}
