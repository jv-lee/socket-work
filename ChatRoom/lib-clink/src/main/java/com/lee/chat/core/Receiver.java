package com.lee.chat.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    boolean receiveAsync(IOArgs.IoArgsEventListener listener) throws IOException;
}
