package com.lee.chat;


import com.lee.chat.handler.ClientHandler;
import com.lee.chat.utils.CloseUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener listener;
    //创建线程安全的List 只能解决 remove 时 add不会出现问题. 解决遍历问题需要使用同步块
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;
    private Selector selector;
    ServerSocketChannel server;

    public TCPServer(int port) {
        this.port = port;
        //转发线程池
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            //设置为非阻塞状态
            server.configureBlocking(false);
            //绑定本地端口
            server.socket().bind(new InetSocketAddress(port));

            //注册客户端连接通知事件
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("服务器信息：" + server.getLocalAddress().toString());

            //启动客户端监听
            this.listener = new ClientListener();
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }

        CloseUtils.close(server);
        CloseUtils.close(selector);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.exit();
            }
            clientHandlers.clear();
        }
        //停止线程池
        forwardingThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        //打印消息
        System.out.println("Received-" + handler.getClientInfo() + ":" + msg);

        //异步提交转发任务
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlers) {
                    if (clientHandler.equals(handler)) {
                        //跳过自己
                        continue;
                    }
                    //对其他客户端发送消息
                    clientHandler.send(msg);
                }
            }
        });
    }

    private class ClientListener extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            super.run();
            Selector selector = TCPServer.this.selector;
            System.out.println("服务器准备就绪~");
            do {
                try {
                    //等待客户端连接
                    if (selector.select() == 0) {
                        if (done) break;
                        continue;
                    }

                    //获取索引 判断是否有链接
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) break;
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        //检查当前key的状态是否是我们关注的客户端到达状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            //非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();

                            try {
                                //客户端构建异步线程
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this);
                                //读取数据并打印
                                clientHandler.readToPrint();
                                //添加同步处理
                                synchronized (TCPServer.this) {
                                    clientHandlers.add(clientHandler);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } while (!done);

            System.out.println("服务器已关闭!");
        }

        public void exit() {
            done = true;
            //唤醒当前的阻塞
            selector.wakeup();
        }

    }


}
