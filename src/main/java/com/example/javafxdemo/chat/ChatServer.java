package com.example.javafxdemo.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static final int DEFAULT_PORT = 5555;
    static final Map<String, ClientHandler> CLIENTS = new ConcurrentHashMap<>();

    @SuppressWarnings("resource")
    public static void startInBackground(int port) {
        ServerSocket socket;
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Server not started on port " + port + ": " + e.getMessage());
            return;
        }
        Thread t = new Thread(() -> acceptLoop(socket), "chat-server-accept");
        t.setDaemon(true);
        t.start();
        System.out.println("Chat server listening on port " + port);
    }

    private static void acceptLoop(ServerSocket serverSocket) {
        try {
            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client);
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Server accept loop stopped: " + e.getMessage());
        }
    }

    static void broadcastUserList() {
        String users = String.join(",", CLIENTS.keySet());
        String payload = "USERS|" + users;
        for (ClientHandler h : CLIENTS.values()) {
            h.send(payload);
        }
    }
}
