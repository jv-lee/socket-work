package com.lee.chat;


import com.lee.chat.constants.TCPConstants;
import com.lee.chat.core.IOContext;
import com.lee.chat.imple.IOSelectorProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("server");
        IOContext.setup()
                .ioProvider(new IOSelectorProvider())
                .start();

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER,cachePath);
        boolean isSucceed = tcpServer.start();

        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);

        //服务端读取任意输入操作 结束服务监听
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            tcpServer.broadcast(str);
        } while (!"00bye00".equalsIgnoreCase(str));

        UDPProvider.stop();
        tcpServer.stop();
        IOContext.close();
    }
}
