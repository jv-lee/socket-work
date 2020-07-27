package com.lee.chat.handler;


import com.lee.chat.Foo;
import com.lee.chat.core.Connector;
import com.lee.chat.core.Packet;
import com.lee.chat.core.ReceivePacket;
import com.lee.chat.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector {
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;
    private final File cachePath;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback,File cachePath) throws IOException {
        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;

        System.out.println("新客户端连接：" + clientInfo);

        setup(socketChannel);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTempFile(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket packet) {
        super.onReceivedPacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) packet.entity();
            System.out.println(key.toString() + ":" + string);
            clientHandlerCallback.onNewMessageArrived(this, string);
        }
    }

    public void exit() {
        CloseUtils.close(this);
        System.out.println("客户端已退出：" + clientInfo);
    }

    private void exitBySelf() {
        exit();
        //通知外界自己已经启动关闭
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        /**
         * 自身关闭时通知回调
         *
         * @param handler
         */
        void onSelfClosed(ClientHandler handler);

        /**
         * 收到新消息时的回调通知
         *
         * @param handler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler handler, String msg);
    }

}

