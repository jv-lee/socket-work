package com.lee.chat.imple.async;

import com.lee.chat.box.StringReceivePacket;
import com.lee.chat.core.*;
import com.lee.chat.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher, IOArgs.IoArgsEventProcessor {
    private final Receiver receiver;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final ReceivePacketCallback callback;

    private IOArgs ioArgs = new IOArgs();
    private ReceivePacket<?, ?> packetTemp;

    private WritableByteChannel packetChannel;
    private long total;
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveProcessor(this);
        this.callback = callback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            completePacket(false);
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 解析数据到Packet
     *
     * @param args
     */
    private void assemblePacket(IOArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            byte type = length > 200 ? Packet.TYPE_STREAM_FILE : Packet.TYPE_MEMORY_STRING;

            packetTemp = callback.onArrivedNewPacket(type, length);
            packetChannel = Channels.newChannel(packetTemp.open());

            total = length;
            position = 0;
        }

        try {
            int count = args.writeTo(packetChannel);
            position += count;

            if (position == total) {
                completePacket(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            completePacket(false);
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket(boolean isSucceed) {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        packetTemp = null;

        WritableByteChannel channel = this.packetChannel;
        CloseUtils.close(channel);
        packetChannel = null;

        if (packet != null) {
            callback.onReceivePacketCompleted(packet);
        }
    }

    @Override
    public IOArgs provideIoArgs() {
        IOArgs args = ioArgs;

        int receiveSize;
        if (packetTemp == null) {
            receiveSize = 4;
        } else {
            receiveSize = (int) Math.min(total - position, args.capacity());
        }
        //设置本次接收数据大小
        args.limit(receiveSize);
        return args;
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        assemblePacket(args);
        //继续接收下一条数据
        registerReceive();
    }
}
