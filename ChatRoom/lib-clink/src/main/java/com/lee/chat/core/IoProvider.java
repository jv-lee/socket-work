package com.lee.chat.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {

    boolean registerInput(SocketChannel channel, HandlerInputCallback callback);

    boolean registerOutput(SocketChannel channel, HandlerOutputCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandlerInputCallback implements Runnable {
        @Override
        public void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    abstract class HandlerOutputCallback implements Runnable {
        private Object attach;

        @Override
        public void run() {
            canProviderOutput();
        }

        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        protected abstract void canProviderOutput();
    }
}
