import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @author jv.lee
 * @date 2020-06-23
 * @description UDP 提供者，用于内容提供服务
 */
public class UDPProvider {
    public static void main(String[] args) throws IOException {
        System.out.println("UDPProvider Started.");

        //作为接收者，指定一个端口用于数据接收
        DatagramSocket ds = new DatagramSocket(20000);

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
        System.out.println("UDPProvider receive form ip:" + ip + ":" + port);
        System.out.println("data:" + data);

        //构建回送数据
        String response = "Receive data with length:" + dataLength;
        byte[] responseBytes = response.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, receivePacket.getAddress(), receivePacket.getPort());
        ds.send(responsePacket);

        System.out.println("UDPProvider Finished.");
        ds.close();
    }
}
