package com.lee.chat.imple;

import com.lee.chat.core.IOArgs;
import com.lee.chat.core.IOProvider;
import com.lee.chat.core.Receiver;
import com.lee.chat.core.Sender;
import com.lee.chat.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IOProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IOArgs.IoArgsEventListener receiveIOEventListener;
    private IOArgs.IoArgsEventListener sendIOEventListener;

    private IOArgs receiveArgsTemp;

    public SocketChannelAdapter(SocketChannel channel, IOProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IOArgs.IoArgsEventListener listener) {
        receiveIOEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IOArgs args) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        receiveArgsTemp = args;
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public boolean sendAsync(IOArgs args, IOArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        sendIOEventListener = listener;
        //设置当前需要发送的数据 附加到回调中
        outputCallback.setAttach(args);
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void close() throws IOException {
        //当前状态是否未false 关闭状态， 如果是则设置未true
        if (isClosed.compareAndSet(false, true)) {
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            CloseUtils.close(channel);
            //回调通知channel已被关闭
            listener.onChannelClosed(channel);
        }
    }

    private final IOProvider.HandlerInputCallback inputCallback = new IOProvider.HandlerInputCallback() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IOArgs args = receiveArgsTemp;
            IOArgs.IoArgsEventListener listener = receiveIOEventListener;
            listener.onStarted(args);

            try {
                //具体读取操作
                if (args.readFrom(channel) > 0) {
                    //读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot readFrom any data!");
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IOProvider.HandlerOutputCallback outputCallback = new IOProvider.HandlerOutputCallback() {
        @Override
        protected void canProviderOutput() {
            if (isClosed.get()) {
                return;
            }

            IOArgs args = getAttach();
            IOArgs.IoArgsEventListener listener = sendIOEventListener;

            listener.onStarted(args);

            try {
                //具体写入操作
                if (args.writeTo(channel) > 0) {
                    //写入完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot writeTo any data!");
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
