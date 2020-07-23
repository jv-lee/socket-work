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

    private IOArgs.IoArgsEventProcessor receiveIOEventProcessor;
    private IOArgs.IoArgsEventProcessor sendIOEventProcessor;

    public SocketChannelAdapter(SocketChannel channel, IOProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveProcessor(IOArgs.IoArgsEventProcessor processor) {
        receiveIOEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public void setSenderProcessor(IOArgs.IoArgsEventProcessor processor) {
        sendIOEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
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

            IOArgs.IoArgsEventProcessor processor = receiveIOEventProcessor;
            IOArgs args = processor.provideIoArgs();

            try {
                //具体读取操作
                if (args.readFrom(channel) > 0) {
                    //读取完成回调
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args, new IOException("Cannot readFrom any data!"));
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

            IOArgs.IoArgsEventProcessor processor = sendIOEventProcessor;
            IOArgs args = processor.provideIoArgs();

            try {
                //具体写入操作
                if (args.writeTo(channel) > 0) {
                    //写入完成回调
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args, new IOException("Cannot writeTo any data!"));
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
