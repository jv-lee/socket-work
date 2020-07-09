package server;

import constants.TCPConstants;

import java.io.IOException;

public class Server {
    public static void main(String[] args) {
        ServerProvider.start(TCPConstants.PORT_SERVER);

        try {
            //服务端读取任意输入操作 结束服务监听
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ServerProvider.stop();
    }
}
