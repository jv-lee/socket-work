import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author jv.lee
 * @date 2020-06-22
 * @description 服务器端
 */
public class Server {
    private static final int PORT = 20000;

    public static void main(String[] args) throws IOException {
        ServerSocket server = createServerSocket();

        initServerSocket(server);

        //绑定到本地端口上
        server.bind(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 50);

        System.out.println("服务器准备就绪~");
        System.out.println("服务器信息：" + server.getInetAddress() + ":" + server.getLocalPort());

        //等待客户端连接
        for (; ; ) {
            //得到客户端 accept是阻塞方法， 未获取到client时是阻塞状态
            Socket client = server.accept();
            ClientHandler clientHandler = new ClientHandler(client);
            clientHandler.start();
        }
    }

    private static ServerSocket createServerSocket() throws IOException {
        //创建基础的ServerSocket
        ServerSocket serverSocket = new ServerSocket();

        //绑定到本地端口20000上，并且设置当前可允许等待链接的队列为50个
//        serverSocket = new ServerSocket(PORT);

        //等价上述构造函数
//        serverSocket = new ServerSocket(PORT, 50);

        //等价上述构造函数
//        serverSocket = new ServerSocket(PORT, 50, Inet4Address.getLocalHost());

        return serverSocket;
    }

    private static void initServerSocket(ServerSocket serverSocket) throws IOException {
        //是否复用未完全关闭的地址端口
        serverSocket.setReuseAddress(true);

        //等效Socket#setReceiveBufferSize (对accept接收到的客户端socket 设置bufferSize)
        serverSocket.setReceiveBufferSize(64 * 1024 * 1024);

        //设置serverSocket#accept超时时间 默认不设置，永久等待客户端连接
//        serverSocket.setSoTimeout(2000);

        //设置性能参数：短链接、延迟、带宽的相对重要性
        serverSocket.setPerformancePreferences(1, 1, 1);
    }

    private void todo(Socket client) {

    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private boolean flag = true;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("新客户端连接：" + socket.getInetAddress() + ":" + socket.getPort());

            try {
                //得到打印流，用于数据输出；服务器回送数据使用
                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                do {
                    //从客户端获取数据
                    String str = socketInput.readLine();
                    if ("bye".equalsIgnoreCase(str)) {
                        flag = false;
                        //回送一条数据
                        socketOutput.println("bye");
                    } else {
                        System.out.println(str);
                        socketOutput.println("回送：" + str.length());
                    }

                } while (flag);

                socketInput.close();
                socketOutput.close();

            } catch (Exception e) {
                System.out.println("连接异常：" + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("客户端已退出:" + socket.getInetAddress() + ":" + socket.getPort());
        }

    }

}
