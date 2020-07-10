package server;

import constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) {
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();

        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
        } else {
            System.out.println("Start TCP server success!");
        }
        UDPProvider.start(TCPConstants.PORT_SERVER);

        try {
            //服务端读取任意输入操作 结束服务监听
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String str;
            do {
                str = bufferedReader.readLine();
                tcpServer.broadcast(str);
            } while (!"00bye00".equalsIgnoreCase(str));

        } catch (IOException e) {
            e.printStackTrace();
        }

        UDPProvider.stop();
        tcpServer.stop();
    }
}
