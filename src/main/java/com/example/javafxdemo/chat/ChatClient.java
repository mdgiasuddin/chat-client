package com.example.javafxdemo.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChatClient {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public void send(String msg) {
        if (writer != null) {
            writer.println(msg);
        }
    }

    public void startListening(Consumer<String> onMessage) {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
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
