package com.lee.chat.core;

import com.lee.chat.box.StringReceivePacket;
import com.lee.chat.box.StringSendPacket;
import com.lee.chat.imple.SocketChannelAdapter;
import com.lee.chat.imple.async.AsyncReceiveDispatcher;
import com.lee.chat.imple.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;
    private ReceivePacket receivePacket;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;

        IOContext context = IOContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        //启动接收
        receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }


    protected void onReceiveNewMessage(String str) {
        System.out.println(key.toString() + ":" + str);
    }

    protected void onReceivePacket(ReceivePacket packet) {
        System.out.println(key.toString() + ":[New Packet]-Type:" + packet.type() + ",Length:" + packet.length);
    }

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = this::onReceivePacket;
}
