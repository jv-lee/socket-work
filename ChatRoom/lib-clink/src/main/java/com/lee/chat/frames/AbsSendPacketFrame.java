package com.lee.chat.frames;

import com.lee.chat.core.SendPacket;

/**
 * @author jv.lee
 * @date 2020/8/17
 * @description
 */
public abstract class AbsSendPacketFrame extends AbsSendFrame{
    protected SendPacket<?> packet;
    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }
}
