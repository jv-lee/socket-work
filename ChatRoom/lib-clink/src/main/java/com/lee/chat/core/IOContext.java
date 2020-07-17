package com.lee.chat.core;

import java.io.IOException;

public class IOContext {
    private static IOContext INSTANCE;
    private final IOProvider ioProvider;

    private IOContext(IOProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IOProvider getIoProvider() {
        return ioProvider;
    }

    public static IOContext get() {
        return INSTANCE;
    }

    public static StartedBoot setup() {
        return new StartedBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
    }

    public static class StartedBoot {
        private IOProvider ioProvider;

        private StartedBoot() {
        }

        public StartedBoot ioProvider(IOProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IOContext start() {
            INSTANCE = new IOContext(ioProvider);
            return INSTANCE;
        }
    }

}
