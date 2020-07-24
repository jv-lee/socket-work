package com.lee.chat;

import com.lee.chat.bean.ServerInfo;
import com.lee.chat.box.FileSendPacket;
import com.lee.chat.core.IOContext;
import com.lee.chat.imple.IOSelectorProvider;

import java.io.*;

public class Client {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        IOContext.setup()
                .ioProvider(new IOSelectorProvider())
                .start();

        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;

            try {
                tcpClient = TCPClient.startWith(info, cachePath);
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

        IOContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

            //--f url
            if (str.startsWith("--f")) {
                String[] array = str.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send();
                    }
                }
            }

            // 发送到服务器 TODO 测试所以有4次发送 正常需修改为1次
            tcpClient.send(str);
            tcpClient.send(str);
            tcpClient.send(str);
            tcpClient.send(str);

        } while (true);
    }

}
