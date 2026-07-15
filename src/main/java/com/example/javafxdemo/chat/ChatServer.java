package com.example.javafxdemo.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static final int DEFAULT_PORT = 5555;

    private static final Map<String, ClientHandler> CLIENTS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        ServerSocket socket = new ServerSocket(port);
        System.out.println("Chat server listening on port " + port);
        acceptLoop(socket);
    }

    public static void startInBackground(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            Thread t = new Thread(() -> acceptLoop(socket), "chat-server-accept");
            t.setDaemon(true);
            t.start();
            System.out.println("Chat server listening on port " + port);
        } catch (IOException e) {
            System.out.println("Exception occurs: " + e.getMessage());
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

    private static void broadcastUserList() {
        String users = String.join(",", CLIENTS.keySet());
        String payload = "USERS|" + users;
        for (ClientHandler h : CLIENTS.values()) {
            h.send(payload);
        }
    }

    private static final class ClientHandler extends Thread {

        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        ClientHandler(Socket socket) {
            super("chat-client-" + socket.getRemoteSocketAddress());
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (IOException ignored) {
            } finally {
                if (username != null) {
                    CLIENTS.remove(username, this);
                    broadcastUserList();
                }
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void handle(String line) {
            String[] parts = line.split("\\|", 4);
            String cmd = parts[0];
            switch (cmd) {
                case "LOGIN" -> handleLogin(parts);
                case "MSG" -> handleMessage(parts);
                case "LOGOUT" -> {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
                default -> {
                }
            }
        }

        private void handleLogin(String[] parts) {
            if (parts.length < 2 || parts[1].isBlank()) {
                send("LOGIN_FAIL|Empty username");
                return;
            }
            String requested = parts[1].trim();
            if (CLIENTS.putIfAbsent(requested, this) != null) {
                send("LOGIN_FAIL|Username already taken");
                return;
            }
            username = requested;
            send("LOGIN_OK");
            broadcastUserList();
        }

        private void handleMessage(String[] parts) {
            if (parts.length < 4 || username == null) {
                return;
            }
            String to = parts[2];
            String text = parts[3];
            ClientHandler target = CLIENTS.get(to);
            if (target != null) {
                target.send("MSG|" + username + "|" + to + "|" + text);
            }
        }

        void send(String msg) {
            PrintWriter w = out;
            if (w != null) {
                w.println(msg);
            }
        }
    }
}
