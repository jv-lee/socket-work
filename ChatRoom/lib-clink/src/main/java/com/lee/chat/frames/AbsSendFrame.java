package com.lee.chat.frames;

import com.lee.chat.core.Frame;
import com.lee.chat.core.IOArgs;

import java.io.IOException;

/**
 * @author jv.lee
 * @date 2020-07-29
 * @description
 */
public abstract class AbsSendFrame extends Frame {
    byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemaining = length;
    }

    @Override
    public boolean handle(IOArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();

            if (headerRemaining > 0 && args.remaining()) {
                headerRemaining -= consumeHeader(args);
            }

            if (headerRemaining == 0 && args.remaining() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(args);
            }

            //全部消费完成
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    protected abstract byte consumeHeader(IOArgs args) throws IOException;

    protected abstract int consumeBody(IOArgs args) throws IOException;
}
