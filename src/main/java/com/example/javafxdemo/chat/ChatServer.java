package com.example.javafxdemo.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static final int DEFAULT_PORT = 5555;
    static final Map<String, ClientHandler> CLIENTS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Chat server listening on port " + port);
            acceptLoop(socket);
        }
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
