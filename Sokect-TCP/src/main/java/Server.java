import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author jv.lee
 * @date 2020-06-22
 * @description 服务器端
 */
public class Server {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(2000);

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
