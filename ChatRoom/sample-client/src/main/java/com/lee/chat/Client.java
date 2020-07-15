package com.lee.chat;


import com.lee.chat.bean.ServerInfo;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info);
                if (tcpClient == null) {
                    return;
                }
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
    }

    private static void write(TCPClient tcpClient) throws IOException {
        //构建键盘输入流
        InputStream is = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        do {
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            tcpClient.send(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

        } while (true);
    }

}
