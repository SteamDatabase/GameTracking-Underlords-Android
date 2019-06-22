package com.valvesoftware;

public class runnableNative implements Runnable {
    public long m_nativePointer;

    public static native void runNative(long j);

    public runnableNative(long j) {
        this.m_nativePointer = j;
    }

    public void run() {
        runNative(this.m_nativePointer);
    }
}
