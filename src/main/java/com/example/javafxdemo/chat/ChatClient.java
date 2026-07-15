package com.example.javafxdemo.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    public void send(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public void startListening(Consumer<String> onMessage) {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    onMessage.accept(line);
                }
            } catch (IOException ignored) {
            } finally {
                onMessage.accept("DISCONNECTED");
            }
        }, "chat-listener");
        t.setDaemon(true);
        t.start();
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
