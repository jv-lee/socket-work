package com.lee.chat.core;

import java.io.IOException;

/**
 * @author jv.lee
 * @date 2020-07-29
 * @description 每一帧数据转换类
 */
public abstract class Frame {
    public static final int FRAME_HEADER_LENGTH = 6;
    public static final int MAX_CAPACITY = 64 * 1024 - 1; //64k - 1个字节

    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;
    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte FLAG_NONE = 0;


    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifier) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("");
        }

        if (identifier < 1 || identifier > 255) {
            throw new RuntimeException("");
        }

        // 00000000 00000000 00000000 >> 00000000 移除后8位
        header[0] = (byte) (length >> 8);
        header[1] = (byte) (8); // 00000000 被移除的8位

        header[2] = type;
        header[3] = flag;

        header[4] = (byte) identifier; // 取最后的一个字节 8个位
        header[5] = 0;
    }

    /**
     * header[0] = 11111111 1111111 11111111 00100000 (length(00000000 00000000 0010000000 0100000000) >> 8) 向右边移动8位 强转byte后自动取末尾 8位 既位 00100000
     * header[1] = 11111111 1111111 11111111 01000000 (length(00000000 00000000 0010000000 0100000000) 取8位 byte 即为 01000000
     * 获取长度 合并 header[0] 和 header[1] 即为 header[0] & 0xff 运算 所有高位补齐的1 变为0 则为 00000000 00000000 00000000 00100000 << 8 = 00000000 00000000 00100000 00000000 | 00000000 00000000 00000000 01000000 = 000000000 00000000 00100000 01000000
     *
     * @return
     */
    public int getBodyLength() {
        // header[0] 转换为int后 自动转为 11111111 11111111 11111111 01000000 & 0xFF后为 00000000 000000000 00000000 01000000
        // header[1] 转换为int后 自动转为 11111111 11111111 11111111 01000000 & 0xFF后为 00000000 000000000 00000000 01000000
        //已知道 header[0]的8个位 处于 第三区间 17 - 24位 向 << 8 移动8位 为 00000000 00000000 01000000 00000000
        //最后  00000000 00000000 01000000 00000000 | 00000000 00000000 00000000 01000000 = 00000000 00000000 01000000 01000000 即获得长度
        return ((((int) header[0]) & 0xFF) << 8) | (((int) header[1]) & 0xFF);
    }

    public byte getBodyType() {
        return header[2];
    }

    public byte getBodyFlag() {
        return header[3];
    }

    public short getBodyIdentifier() {
        return (short) ((short) header[4] & 0xFF);
    }

    public abstract boolean handle(IOArgs args) throws IOException;

    public abstract Frame nextFrame();

}
