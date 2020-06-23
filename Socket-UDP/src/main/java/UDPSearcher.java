import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author jv.lee
 * @date 2020-06-23
 * @description UDP 搜索者，用于搜索服务支持方
 */
public class UDPSearcher {
    //监听所有设备回传信息端口
    private static final int LISTEN_PORT = 30000;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("UDPSearcher Started.");

        Listener listen = listen();
        sendBroadcast();

        System.in.read();

        List<Device> devicesAndClose = listen.getDevicesAndClose();

        for (Device device : devicesAndClose) {
            System.out.println("Device: " + device.toString());
        }

        System.out.println("UDPSearcher Finished.");
    }

    private static Listener listen() throws InterruptedException {
        System.out.println("UDPSearcher start listen.");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, countDownLatch);
        listener.start();

        countDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDPSearcher sendBroadcast started.");

        //搜索方无需指定端口，让系统直接分配
        DatagramSocket ds = new DatagramSocket();


        //构建一份请求数据
        String requestData = MessageCreator.buildeWithPort(LISTEN_PORT);
        byte[] requestBytes = requestData.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length);

        //20000端口 广播地址
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(20000);

        ds.send(requestPacket);
        ds.close();

        System.out.println("UDPSearcher sendBroadcast finished.");
    }

    private static class Device {
        final int port;
        final String ip;
        final String sn;

        Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch countDownLatch;
        private final List<Device> devices = new ArrayList<>();
        private boolean done = false;
        private DatagramSocket ds = null;

        Listener(int listenPort, CountDownLatch countDownLatch) {
            super();
            this.listenPort = listenPort;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            super.run();
            //通知已启动
            countDownLatch.countDown();

            try {
                //监听回送端口
                ds = new DatagramSocket(listenPort);

                while (!done) {
                    //构建接收实体
                    final byte[] buf = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

                    //接收
                    ds.receive(receivePacket);

                    //打印接收到的信息与发送者的信息
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLength = receivePacket.getLength();
                    String data = new String(receivePacket.getData(), 0, dataLength);
                    System.out.println("UDPSearcher receive form ip:" + ip + ":" + port + " -> data:" + data);

                    String sn = MessageCreator.parseSn(data);
                    if (sn != null) {
                        Device device = new Device(port, ip, sn);
                        devices.add(device);
                    }
                }

            } catch (Exception ignored) {
            } finally {
                close();
            }
            System.out.println("UDPSearcher listener finished.");
        }


        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<Device> getDevicesAndClose() {
            done = true;
            close();
            return devices;
        }

    }

}
