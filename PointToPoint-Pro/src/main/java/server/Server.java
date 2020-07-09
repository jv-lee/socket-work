package server;

import constants.TCPConstants;

import java.io.IOException;

public class Server {
    public static void main(String[] args) {
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();

        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
        }else{
            System.out.println("Start TCP server success!");
        }
        UDPProvider.start(TCPConstants.PORT_SERVER);

        try {
            //服务端读取任意输入操作 结束服务监听
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UDPProvider.stop();
        tcpServer.stop();
    }
}
