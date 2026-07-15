package com.example.javafxdemo.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class ClientHandler extends Thread {

    private final Socket socket;
    private PrintWriter out;
    private String username;

    ClientHandler(Socket socket) {
        super("chat-client-" + socket.getRemoteSocketAddress());
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            String line;
            while ((line = in.readLine()) != null) {
                handle(line);
            }
        } catch (IOException ignored) {
        } finally {
            if (username != null) {
                ChatServer.CLIENTS.remove(username, this);
                ChatServer.broadcastUserList();
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
        if (ChatServer.CLIENTS.putIfAbsent(requested, this) != null) {
            send("LOGIN_FAIL|Username already taken");
            return;
        }
        username = requested;
        send("LOGIN_OK");
        ChatServer.broadcastUserList();
    }

    private void handleMessage(String[] parts) {
        if (parts.length < 4 || username == null) {
            return;
        }
        String to = parts[2];
        String text = parts[3];
        ClientHandler target = ChatServer.CLIENTS.get(to);
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
