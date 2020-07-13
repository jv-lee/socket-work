package net.lee.chat;


import net.lee.chat.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {
    private final int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlers = new ArrayList<>();

    public TCPServer(int port) {
        this.port = port;
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

        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.exit();
        }

        clientHandlers.clear();
    }

    public void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.send(str);
        }
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
                    clientHandler = new ClientHandler(client, handler -> {
                        clientHandlers.remove(handler);
                    });
                    //读取数据并打印
                    clientHandler.readToPrint();
                    clientHandlers.add(clientHandler);
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
