package com.lee.chat.imple.async;

import com.lee.chat.core.IOArgs;
import com.lee.chat.core.SendDispatcher;
import com.lee.chat.core.SendPacket;
import com.lee.chat.core.Sender;
import com.lee.chat.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher, IOArgs.IoArgsEventProcessor {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private IOArgs ioArgs = new IOArgs();
    private SendPacket<?> packetTemp;

    private ReadableByteChannel packetChannel;
    //当前发送包大小
    private long total;
    //当前发送包进度
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSenderProcessor(this);
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            //已取消，不用发送 递归获取下一条
            return takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        SendPacket temp = packetTemp;
        if (temp != null) {
            CloseUtils.close(temp);
        }

        SendPacket packet = packetTemp = takePacket();
        if (packet == null) {
            //队列为空，取消发送状态
            isSending.set(false);
            return;
        }

        total = packet.length();
        position = 0;

        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        if (position >= total) {
            completePacket(position == total);
            sendNextPacket();
            return;
        }

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    private void completePacket(boolean isSucceed) {
        SendPacket packet = this.packetTemp;
        if (packet == null) {
            return;
        }
        CloseUtils.close(packet, packetChannel);
        packetTemp = null;
        packetChannel = null;
        total = 0;
        position = 0;
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            //遗产关闭导致完成操作
            completePacket(false);
        }
    }

    @Override
    public IOArgs provideIoArgs() {
        IOArgs args = ioArgs;
        if (packetChannel == null) {
            packetChannel = Channels.newChannel(packetTemp.open());
            args.limit(4);
            args.writeLength((int) packetTemp.length());
        } else {
            args.limit((int) Math.min(args.capacity(), total - position));
            try {
                int count = args.readFrom(packetChannel);
                position += count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return args;
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        sendCurrentPacket();
    }
}
