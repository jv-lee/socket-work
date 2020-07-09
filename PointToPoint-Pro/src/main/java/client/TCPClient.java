package client;

import client.bean.ServerInfo;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author jv.lee
 * @date 2020-07-09
 * @description
 */
public class TCPClient {
    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        //超时时间
        socket.setSoTimeout(3000);

        //连接本地，端口2000；超时时间3000ms 应使用info获取的address ，但是mac会直接把地址转换成 127.0.0.1 本地地址 所以直接使用获取本地ipv4 address地址
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), info.getPort()), 3000);

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socket.getLocalAddress() + ":" + socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + ":" + socket.getPort());

        try {
            //发送数据
            todo(socket);
        } catch (Exception e) {
            System.out.println("异常关闭:");
        }

        //释放资源
        socket.close();
        System.out.println("客户端已退出~");
    }

    private static void todo(Socket client) throws IOException {
        //构建键盘输入流
        InputStream is = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        //得到Socket输出流
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        //得到Socket输入流，并转换为BufferedReader
        InputStream inputStream = client.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = true;

        do {
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            socketPrintStream.println(str);

            //从服务器读取一行
            String echo = socketBufferedReader.readLine();
            if ("bye".equalsIgnoreCase(echo)) {
                flag = false;
            } else {
                System.out.println(echo);
            }
        } while (flag);

        socketPrintStream.close();
        socketBufferedReader.close();

    }
}
