package com.wzb.interfaces;

import com.wzb.util.RingBuffer;

public interface StoreFactory {
    Store getStore(RingBuffer ringBuffere);
}
