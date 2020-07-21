package com.lee.chat.imple.async;

import com.lee.chat.core.IOArgs;
import com.lee.chat.core.SendDispatcher;
import com.lee.chat.core.SendPacket;
import com.lee.chat.core.Sender;
import com.lee.chat.utils.CloseUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();

    private IOArgs ioArgs = new IOArgs();
    private SendPacket currentPacket;

    //当前发送包大小
    private int total;
    //当前发送包进度
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
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
        SendPacket temp = currentPacket;
        if (temp != null) {
            CloseUtils.close(temp);
        }

        SendPacket packet = currentPacket = takePacket();
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
        IOArgs args = ioArgs;
    }

}
