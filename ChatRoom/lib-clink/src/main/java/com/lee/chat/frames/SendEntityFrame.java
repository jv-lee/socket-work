package com.lee.chat.frames;

import com.lee.chat.core.Frame;
import com.lee.chat.core.IOArgs;
import com.lee.chat.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @author jv.lee
 * @date 2020-07-29
 * @description
 */
public class SendEntityFrame extends AbsSendPacketFrame {
    private final ReadableByteChannel channel;
    private final long unConsumeEntityLength;

    SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel, SendPacket<?> packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY), Frame.TYPE_PACKET_ENTITY, Frame.FLAG_NONE, identifier, packet);
        this.channel = channel;
        unConsumeEntityLength = entityLength - bodyRemaining;

    }

    @Override
    protected int consumeBody(IOArgs args) throws IOException {
        if (packet == null) {
            //已经终止帧，则填充假数据
            return args.fillEmpty(bodyRemaining);
        }
        return args.readFrom(channel);
    }

    @Override
    public Frame nextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
    }
}
