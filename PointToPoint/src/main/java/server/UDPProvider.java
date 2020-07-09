package server;

import constants.CommandConstants;
import constants.UDPConstants;
import net.utils.ByteUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {
    private static Provider PROVIDER_INSTANCE;

    public static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    public static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        //存储消息的Buffer
        final byte[] buffer = new byte[128];

        Provider(String sn, int port) {
            super();
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("ServerProvider Started.");

            try {
                //设置监听 服务端默认端口
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                //接收消息的packet
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    //接收
                    ds.receive(receivePack);

                    //打印接收者的信息与发送者的信息
                    //发送者的IP地址
                    String clientIp = receivePack.getAddress().getHostAddress();
                    int clientPort = receivePack.getPort();
                    int clientDataLen = receivePack.getLength();
                    byte[] clientData = receivePack.getData();
                    //是否为有效数据， 客户端发送数据必须带有请求头+2个字节的sort common指令 及4个字节int的回送端口 && 数据开头必须为请求头口令
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4) && ByteUtils.startsWith(clientData, UDPConstants.HEADER);

                    System.out.println("ServerProvider receiver form ip:" + clientIp + " port:" + clientPort + " dataValid:" + isValid);
                    if (!isValid) {
                        //无效继续
                        continue;
                    }

                    //解析命令与回送端口
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int responsePort = (((clientData[index++]) << 24) |
                            ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            ((clientData[index] & 0xff)));

                    //判断合法性 cmd == 1为搜索命令 回送端口大于0为合法地址
                    if (cmd == CommandConstants.CMD_SEARCH && responsePort > 0) {
                        //构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);//数据头
                        byteBuffer.putShort((short) CommandConstants.CMD_RESPONSE);//回送指令 cmd == 2
                        byteBuffer.putInt(port);//回送端口
                        byteBuffer.put(sn);//数据内容
                        int len = byteBuffer.position();
                        //直接根据发送者构建一份回送信息
                        DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePack.getAddress(), responsePort);
                        ds.send(responsePacket);
                        System.out.println("ServerProvider response to :" + clientIp + ":" + clientPort + " dataLen:" + len);
                    } else {
                        System.out.println("ServerProvider receive cmd nonsupport; cmd:" + cmd + " port:" + port);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                close();
            }
            System.out.println("ServerProvider Finished.");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        public void exit() {
            done = true;
            close();
        }

    }
}
