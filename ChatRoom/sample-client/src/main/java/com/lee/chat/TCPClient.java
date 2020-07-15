package com.lee.chat;


import com.lee.chat.bean.ServerInfo;
import com.lee.chat.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author jv.lee
 * @date 2020-07-09
 * @description
 */
public class TCPClient {
    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    public void send(String msg) {
        printStream.println(msg);
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();

        //连接本地，端口2000；超时时间3000ms 应使用info获取的address ，但是mac会直接把地址转换成 127.0.0.1 本地地址 所以直接使用获取本地ipv4 address地址
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), info.getPort()), 2000);
//        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 2000);


        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socket.getLocalAddress() + ":" + socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + ":" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            return new TCPClient(socket, readHandler);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socket);
        }

        return null;
    }

    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            try {
                //得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str;
                    try {
                        //客户都拿到一条数据
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        //重新等待
                        continue;
                    }
                    if (str == null) {
                        System.out.println("连接已关闭，无法读取数据!");
                        break;
                    }
                    //打印数据
                    System.out.println(str);
                } while (!done);

            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开:" + e.getMessage());
                }
            } finally {
                //连接关闭
                CloseUtils.close(inputStream);
            }

        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }

    }
}
