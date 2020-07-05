package com.wzb.helper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RingBuffer {

    private final static int DEFAULT_SIZE  = 1024;
    public byte[] buffer;
    private int pRead;
    private int pWrite;
    private int bufferSize;
    private int rbCapacity;
    private int time;
    private ReentrantReadWriteLock pReadLock;
    private ReentrantReadWriteLock pWriteLock;
    private AtomicInteger count = new AtomicInteger();
    private AtomicInteger modCount = new AtomicInteger();

    public RingBuffer(){
        this.pRead = 0;
        this.pWrite = 1;
        this.bufferSize = DEFAULT_SIZE;
        this.buffer = new byte[bufferSize];
        this.rbCapacity = bufferSize;
        this.time = 3;
        this.pReadLock = new ReentrantReadWriteLock();
        this.pWriteLock = new ReentrantReadWriteLock();
    }

    public RingBuffer(int initSize, int time){
        this.pRead = 0;
        this.pWrite = 1;
        this.bufferSize = initSize;
        this.buffer = new byte[bufferSize];
        this.time=time;
        this.rbCapacity = bufferSize;
        this.pReadLock = new ReentrantReadWriteLock();
        this.pWriteLock = new ReentrantReadWriteLock();
    }

    public int canRead() {
        if (pRead < pWrite) {
            return pWrite - pRead-1;
        }
        return rbCapacity - (pRead - pWrite)-1;
    }
    public int canWrite() {
        return rbCapacity - canRead()-2;
    }

    public int write(byte[]data, int srcpos,int len, String module) throws InterruptedException
    {
        pWriteLock.writeLock().lock();
        try{
            int waitTime = 0;
            if(len > canWrite()){
                //System.out.printf("space can write: %d , space need write: %d \n",canWrite(),count);
            }
            while(len > canWrite()){
                System.out.printf(module + " space can write: %d , space need write: %d \n",canWrite(),len);
                Thread.sleep(1000);
                //waitTime++;
                //sop("read----sleep:"+i);
               // if(waitTime > 2*time) {
               //     System.out.println(module + ":Write Over Time");
               //     return -1;
               // }
            }

            if (pRead <pWrite) {
                int tailWriteCap = rbCapacity - pWrite;

                if (len <= tailWriteCap) {
                    System.arraycopy(data, srcpos, buffer ,pWrite, len);
                    pWrite += len;
                    if (pWrite == rbCapacity) {
                        pWrite = 0;
                    }
                    return len;
                } else {
                    System.arraycopy(data,srcpos,buffer ,pWrite, tailWriteCap);
                    pWrite = 0;
                    System.arraycopy(data, srcpos+tailWriteCap, buffer, pWrite, len-tailWriteCap);
                    pWrite += len - tailWriteCap;}
            } else {
                System.arraycopy(data,srcpos,buffer ,pWrite, len);
                pWrite += len;
            }
            count.incrementAndGet();
            modCount.incrementAndGet();
            return len;
        }finally {
            pWriteLock.writeLock().unlock();
        }
    }

    public int read(byte[] data,int srcpos ,int count, String module) throws InterruptedException {
        try{
            pReadLock.writeLock().lock();
            int waitTime = 0;
            while (count > canRead()) {
                Thread.sleep(1000);
                waitTime++;
                //sop("write----sleep:"+i);
                if(waitTime>time) {
                    if(canRead() > 0){
                        int lastRead = canRead();
                        // System.out.printf("Read Over Time and read the last %d bytes data; \n", lastRead);
                        System.arraycopy(buffer, pRead+1, data, srcpos, lastRead);
                        pRead += lastRead;
                        //pReadLock.writeLock().unlock();
                        return  lastRead;
                    }else{
                        //System.out.println(module + ": Read Over Time and no data to read");
                        //pReadLock.writeLock().unlock();
                        return -1;
                    }
                }
            }
           // pWriteLock.readLock().lock();
            if (pRead < pWrite) {
                System.arraycopy(buffer, (pRead+1), data, srcpos, count);
                pRead += count;
            }else{
                int tailReadCap = 0;
                tailReadCap = rbCapacity-pRead-1;
                if (count <= tailReadCap) {
                    System.arraycopy(buffer, pRead+1, data, srcpos, count);
                    pRead += count;
                    if (pRead == rbCapacity) {
                        pRead= -1;
                    }
                }else {
                    System.arraycopy(buffer, pRead+1, data, srcpos, tailReadCap);
                    pRead= -1;
                    System.arraycopy(buffer, pRead+1, data, srcpos+tailReadCap, count-tailReadCap);
                    pRead += count-tailReadCap;
                }
            }
            modCount.incrementAndGet();

           // pWriteLock.readLock().unlock();
            pReadLock.writeLock().unlock();
            return count;
        }catch (Exception e){
            System.out.printf("pRead+1 : %d, srcpos: %d, count: %d" , pRead+1, srcpos, count);
            e.printStackTrace();
            return -1;
        }
    }
}

