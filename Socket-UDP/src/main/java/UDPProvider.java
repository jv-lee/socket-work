import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

/**
 * @author jv.lee
 * @date 2020-06-23
 * @description UDP 提供者，用于内容提供服务 处于等待接收消息状态， 搜索器发送广播后 ，所有设备回馈自身信息帮助连接
 */
public class UDPProvider {
    public static void main(String[] args) throws IOException {
        //生成一份唯一标识
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        provider.start();

        System.in.read();
        provider.exit();
    }

    public static class Provider extends Thread {
        private final String sn;
        private boolean done = false;
        private DatagramSocket ds = null;

        Provider(String sn) {
            super();
            this.sn = sn;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPProvider Started.");

            try {
                //监听20000端口
                ds = new DatagramSocket(20000);

                while (!done) {
                    //构建接收实体
                    final byte[] buf = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

                    //设备在此处等待消息接收
                    ds.receive(receivePacket);

                    //打印接收到的信息与发送者的信息
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLength = receivePacket.getLength();
                    String data = new String(receivePacket.getData(), 0, dataLength);
                    System.out.println("UDPProvider receive form ip:" + ip + ":" + port);
                    System.out.println("data:" + data);

                    //解析端口号 创建回送数据发送给搜索器 ， 让搜索器添加当前设备。
                    int responsePort = MessageCreator.parsePort(data);
                    if (responsePort != -1) {
                        //构建回送数据
                        String responseData = MessageCreator.buildWithSn(sn);
                        byte[] responseBytes = responseData.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, receivePacket.getAddress(), responsePort);
                        ds.send(responsePacket);
                    }
                }

            } catch (Exception ignored) {
            } finally {
                close();
            }
            //完成信息
            System.out.println("UDPProvider Finished.");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        /**
         * 提供结束方法
         */
        void exit() {
            done = true;
            close();
        }
    }
}
