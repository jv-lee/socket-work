package net.lee.chat;


import net.lee.chat.bean.ServerInfo;
import net.lee.chat.utils.CloseUtils;

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
    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();

        //连接本地，端口2000；超时时间3000ms 应使用info获取的address ，但是mac会直接把地址转换成 127.0.0.1 本地地址 所以直接使用获取本地ipv4 address地址
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), info.getPort()), 2000);


        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socket.getLocalAddress() + ":" + socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + ":" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            //发送数据
            write(socket);

            //退出操作
            readHandler.exit();
        } catch (Exception e) {
            System.out.println("异常关闭:");
        }

        //释放资源
        socket.close();
        System.out.println("客户端已退出~");
    }

    private static void write(Socket client) throws IOException {
        //构建键盘输入流
        InputStream is = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        //得到Socket输出流
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        boolean flag = true;

        do {
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            socketPrintStream.println(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

        } while (true);

        socketPrintStream.close();
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
