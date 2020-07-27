package com.lee.chat.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {

    // BYTES 类型
    public static final byte TYPE_MEMORY_BYTES = 1;
    // String 类型
    public static final byte TYPE_MEMORY_STRING = 2;
    // 文件 类型
    public static final byte TYPE_STREAM_FILE = 3;
    //长链接流 类型
    public static final byte TYPE_STREAM_DIRECT = 4;

    protected long length;
    private Stream stream;

    protected abstract Stream createStream();

    /**
     * 类型，直接通过方法获得
     * {@link #TYPE_MEMORY_BYTES}
     * {@link #TYPE_MEMORY_STRING}
     * {@link #TYPE_STREAM_FILE}
     * {@link #TYPE_STREAM_DIRECT}
     *
     * @return
     */
    public abstract byte type();

    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    public long length() {
        return length;
    }

    public final Stream open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }

    /**
     * 头部额外信息，用于携带额外校验信息等
     *
     * @return byte 数组,最大255长度
     */
    public byte[] headerInfo() {
        return null;
    }

}
