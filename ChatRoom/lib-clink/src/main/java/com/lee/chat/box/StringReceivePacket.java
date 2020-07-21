package com.lee.chat.box;

import com.lee.chat.core.ReceivePacket;

import java.io.IOException;

public class StringReceivePacket extends ReceivePacket {
    private byte[] buffer;
    private int position;

    public StringReceivePacket(int len) {
        buffer = new byte[len];
        length = len;
    }

    @Override
    public void save(byte[] bytes, int count) {
        //将bytes copy到 buffer中 从0 - count 最后保存position
        System.arraycopy(bytes, 0, buffer, position, count);
        position += count;
    }

    public String string(){
        return new String(buffer);
    }

    @Override
    public void close() throws IOException {

    }
}
