package com.lee.chat.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    void setReceiveProcessor(IOArgs.IoArgsEventProcessor processor);
    boolean postReceiveAsync() throws IOException;
}
