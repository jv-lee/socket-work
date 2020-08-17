package com.lee.chat.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class IOArgs {
    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(256);


    /**
     * 从byte数组中读取数据
     *
     * @param bytes  数据
     * @param offset 偏移量
     * @param count  总长度
     * @return 返回消费大小
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 从byte数组中读取数据
     *
     * @param bytes  数据
     * @param offset 偏移量
     * @return 返回写入大小
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从Channel中读取数据
     *
     * @return 返回消费大小
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }

        finishWriting();
        return bytesProduced;
    }

    /**
     * 写入数据到bytes当中
     *
     * @return
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 从SocketChannel读取数据
     *
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }

        finishWriting();
        return bytesProduced;
    }

    /**
     * 写数据到SocketChannel中去
     *
     * @param channel
     * @return
     * @throws IOException
     */
    public int writeTo(SocketChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 开始写入数据到IOArgs
     */
    public void startWriting() {
        buffer.clear();
        //定义容纳区间
        buffer.limit(limit);
    }

    /**
     * 数据写完后调用
     */
    public void finishWriting() {
        buffer.flip();
    }

    /**
     * 设置单次写操作的容纳区间
     *
     * @param limit 区间大小
     */
    public void limit(int limit) {
        //判断limit 和 最大存储区间中的最小值
        this.limit = Math.min(limit, buffer.capacity());
    }

    public void writeLength(int total) {
        startWriting();
        buffer.putInt(total);
        finishWriting();
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remaining() {
        return buffer.remaining() > 0;
    }

    /**
     * 填充加数据 将buffer的position移动至末尾
     *
     * @param size
     * @return
     */
    public int fillEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        //移动到buffer的最后 类似于填充空数据 再次取数据时 position已经定位到尾部
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * IoArgs 提供者、处理者；数据生产者或消费者
     */
    public interface IoArgsEventProcessor {
        /**
         * 提供一份可消费的IoArgs
         *
         * @return
         */
        IOArgs provideIoArgs();

        /**
         * 消费失败时回调
         *
         * @param args
         * @param e
         */
        void onConsumeFailed(IOArgs args, Exception e);

        /**
         * 消费成功
         *
         * @param args
         */
        void onConsumeCompleted(IOArgs args);
    }

}
