package com.lee.chat.core;

import java.io.InputStream;

/**
 * 发送包的定义
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {
    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    

}
