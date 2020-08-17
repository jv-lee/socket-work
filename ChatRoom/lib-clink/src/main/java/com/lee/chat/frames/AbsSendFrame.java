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
    //设置成员变量为多线程可见
    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemaining = length;
    }

    /**
     * synchronized 设置多线程等待消费，解决多线程消费错乱问题
     *
     * @param args
     * @return
     * @throws IOException
     */
    @Override
    public synchronized boolean handle(IOArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();

            //判断当前存储区间是否大于0 消费header数据
            if (headerRemaining > 0 && args.remaining()) {
                headerRemaining -= consumeHeader(args);
            }

            //判断当前存储空间是否大于0 消费body数据
            if (headerRemaining == 0 && args.remaining() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(args);
            }

            //全部消费完成
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    /**
     * 消费头部信息
     * @param args
     * @return
     */
    private byte consumeHeader(IOArgs args) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte)args.readFrom(header, offset, count);
    }

    protected abstract int consumeBody(IOArgs args) throws IOException;
}
