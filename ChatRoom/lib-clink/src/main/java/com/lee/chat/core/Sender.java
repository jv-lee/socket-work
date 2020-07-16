package com.lee.chat.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    boolean sendAsync(IOArgs args, IOArgs.IoArgsEventListener listener) throws IOException;
}
