package com.lee.chat.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    void setSenderProcessor(IOArgs.IoArgsEventProcessor processor);
    boolean postSendAsync() throws IOException;
}
