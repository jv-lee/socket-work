package com.lee.chat.box;

public class StringSendPacket extends BytesSendPacket {

    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
