package com.lee.chat.core;

/**
 * @author jv.lee
 * @date 2020-07-29
 * @description 每一帧数据转换类
 */
public class Frame {
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

    public int getBodyLength() {
        // 00000000 | 01000000
        // 00000000 000000000 00000000 01000000 想获得的
        // 11111111 11111111 11111111 01000000 实际获得的
        // 使用按位与运算 0xFF -> 00000000 00000000 00000000 11111111 -> 11111111 11111111 11111111 01000000 -> 00000000 000000000 00000000 01000000 (成功获得)
        //最终 00000000 00000000 00000000 00000000 左移8位 00000000 00000000 00000000 + 01000000
        return ((((int) header[0]) & 0xFF) << 8) | (((int) header[1]) & 0xFF);
    }

}
