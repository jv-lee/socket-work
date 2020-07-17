package com.lee.chat.imple;

import com.lee.chat.core.IOProvider;
import com.lee.chat.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IOSelectorProvider implements IOProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    //是否处于注册input中
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    //是否处于注册output中
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlerPool;
    private final ExecutorService outputHandlerPool;

    public IOSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        //构建输入/输出 线程池
        inputHandlerPool = Executors.newFixedThreadPool(4, new IOProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlerPool = Executors.newFixedThreadPool(4, new IOProviderThreadFactory("IoProvider-Output-Thread-"));

        //开始输出输入的监听
        startRead();
        startWrite();
    }

    private void startRead() {
        Thread thread = new Thread("Clink IOSelectorProvider WriteSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(inRegInput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlerPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new Thread("Clink IOSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, selectionKey.OP_WRITE, outputCallbackMap, outputHandlerPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    @Override
    public boolean registerInput(SocketChannel channel, HandlerInputCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandlerOutputCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlerPool.shutdown();
            inputCallbackMap.clear();

            outputHandlerPool.shutdown();
            outputCallbackMap.clear();

            readSelector.wakeup();
            writeSelector.wakeup();

            CloseUtils.close(readSelector, writeSelector);
        }
    }

    /**
     * 等待Selection操作
     *
     * @param locker
     */
    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            //当前是否为锁定状态
            if (locker.get()) {
                try {
                    //锁定状态则开始等待
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker, HashMap<SelectionKey, Runnable> map, Runnable runnable) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            //这是当前为锁定状态
            locker.set(true);

            try {
                //唤醒当前selector,让selector不处于select()状态
                selector.wakeup();

                SelectionKey key = null;
                if (channel.isRegistered()) {
                    //查询是否已经注册过
                    key = channel.keyFor(selector);
                    if (key != null) {
                        //替换注册状态
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {
                    //注册selector 得到key
                    key = channel.register(selector, registerOps);
                    //注册回调
                    map.put(key, runnable);
                }

                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                //解除锁定状态
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                //取消所有key监听
                key.cancel();
                map.remove(key);
                //通知监听下一个key
                selector.wakeup();
            }
        }
    }

    private static void handleSelection(SelectionKey key, int keyOps, HashMap<SelectionKey, Runnable> map, ExecutorService pool) {
        //取消继续对keyOps的监听
        key.interestOps(key.readyOps() & ~keyOps);
        Runnable runnable = null;
        try {
            runnable = map.get(key);
        } catch (Exception ignored) {
        }

        if (runnable != null && !pool.isShutdown()) {
            //异步调度
            pool.execute(runnable);
        }
    }


    static class IOProviderThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IOProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable var1) {
            Thread t = new Thread(this.group, var1, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }

}
