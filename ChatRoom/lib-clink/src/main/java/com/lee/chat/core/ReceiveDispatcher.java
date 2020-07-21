package com.lee.chat.core;

/**
 * 接收的数据调度封装
 * 把一份或者多份IoArgs组合成一份Packet
 */
public interface ReceiveDispatcher {
    void start();

    void stop();

    interface ReceivePacketCallback {
        void onReceivePacketCompleted(ReceivePacket packet);
    }
}
