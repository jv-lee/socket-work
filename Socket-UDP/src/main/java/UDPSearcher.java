import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author jv.lee
 * @date 2020-06-23
 * @description UDP 搜索者，用于搜索服务支持方
 */
public class UDPSearcher {
    public static void main(String[] args) throws IOException {
        System.out.println("UDPSearcher Started.");

        //搜索方无需指定端口，让系统直接分配
        DatagramSocket ds = new DatagramSocket();


        //构建一份请求数据
        String requestData = "HelloWord";
        byte[] requestBytes = requestData.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length);

        //因为是本机测试 发送地址为本机 20000端口
        requestPacket.setAddress(InetAddress.getLocalHost());
        requestPacket.setPort(20000);

        ds.send(requestPacket);

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
        System.out.println("UDPSearcher receive form ip:" + ip + ":" + port);
        System.out.println("data:"+data);


        System.out.println("UDPSearcher Finished.");
        ds.close();
    }
}
