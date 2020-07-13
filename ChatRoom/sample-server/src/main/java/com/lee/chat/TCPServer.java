package com.lee.chat;



import com.lee.chat.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener mListener;
    //创建线程安全的List 只能解决 remove 时 add不会出现问题. 解决遍历问题需要使用同步块
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        //转发线程池
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener listener = new ClientListener(port);
            mListener = listener;
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }
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
        private ServerSocket server;
        private boolean done = false;

        private ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + ":" + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("服务器准备就绪~");

            //等待客户端连接
            do {
                Socket client;
                try {
                    client = server.accept();
                } catch (Exception ignored) {
                    continue;
                }

                //客户端构建异步线程
                ClientHandler clientHandler = null;
                try {
                    clientHandler = new ClientHandler(client, TCPServer.this);
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
            } while (!done);

            System.out.println("服务器已关闭!");
        }

        public void exit() {
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}
