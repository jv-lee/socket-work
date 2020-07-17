package com.lee.chat.handler;


import com.lee.chat.core.Connector;
import com.lee.chat.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Connector connector;
    private final SocketChannel socketChannel;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;

        connector = new Connector() {
            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);
                exitBySelf();
            }

            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);
            }
        };
        connector.setup(socketChannel);


        //监听输出状态
        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);


        this.clientHandlerCallback = clientHandlerCallback;
        clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    public void exit() {
        writeHandler.exit();
        CloseUtils.close(connector, socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }

    private void exitBySelf() {
        exit();
        //通知外界自己已经启动关闭
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        /**
         * 自身关闭时通知回调
         *
         * @param handler
         */
        void onSelfClosed(ClientHandler handler);

        /**
         * 收到新消息时的回调通知
         *
         * @param handler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler handler, String msg);
    }

    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void send(String str) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg) {
                //添加行结束符
                this.msg = msg + '\n';
            }

            @Override
            public void run() {
                if (done) {
                    return;
                }

                //clear后 buffer 指针为0
                byteBuffer.clear();
                //put后指针来到了byte数组的长度位置
                byteBuffer.put(msg.getBytes());
                //反转操作 - 重点 让指针回到初始位置，结束位置为指针位置。
                byteBuffer.flip();

                //未结束 && 是否有数据继续发送
                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        int length = socketChannel.write(byteBuffer);
                        //length = 0 合法
                        if (length < 0) {
                            System.out.println("客户端已经无法发送数据!");
                            ClientHandler.this.exitBySelf();
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }

    }

}

