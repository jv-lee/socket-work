package com.lee.chat.core;

/**
 * 发送包的定义
 */
public abstract class SendPacket extends Packet {
    private boolean canceled;

    public abstract byte[] bytes();

    public boolean isCanceled() {
        return canceled;
    }
}
