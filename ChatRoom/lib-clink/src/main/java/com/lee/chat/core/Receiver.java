package com.lee.chat.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    void setReceiveListener(IOArgs.IoArgsEventListener listener);
    boolean receiveAsync(IOArgs args) throws IOException;
}
