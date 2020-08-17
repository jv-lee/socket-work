package com.lee.chat.frames;

import com.lee.chat.core.Frame;
import com.lee.chat.core.IOArgs;
import com.lee.chat.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author jv.lee
 * @date 2020-07-29
 * @description
 */
public class SendHeaderFrame extends AbsSendPacketFrame {
    private static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket packet) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH, Frame.TYPE_PACKET_HEADER, Frame.FLAG_NONE, identifier, packet);
        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();
        this.body = new byte[bodyRemaining];

        //因为packetLength是long类型 64位 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000
        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (8);

        body[5] = packetType;

        if (packetHeaderInfo != null) {
            System.arraycopy(packetHeaderInfo, 0, body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }

    @Override
    protected int consumeBody(IOArgs args) throws IOException {
        int count = bodyRemaining;
        int offset = body.length - count;
        return args.readFrom(body, offset, count);
    }

    @Override
    public Frame nextFrame() {
        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);
        return new SendEntityFrame(getBodyIdentifier(),packet.length(),channel,packet);
    }
}
