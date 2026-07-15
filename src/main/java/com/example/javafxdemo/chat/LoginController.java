package com.example.javafxdemo.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private Button loginButton;
    @FXML
    private Label statusLabel;

    @FXML
    protected void onLogin() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            statusLabel.setText("Please enter a username.");
            return;
        }
        if (host.isEmpty()) {
            host = "localhost";
        }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port.");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setText("Connecting...");

        final String finalHost = host;
        final int finalPort = port;
        final String finalUser = username;

        Thread t = new Thread(() -> attemptLogin(finalHost, finalPort, finalUser), "chat-login");
        t.setDaemon(true);
        t.start();
    }

    private void attemptLogin(String host, int port, String username) {
        ChatClient client = new ChatClient();
        try {
            client.connect(host, port);
        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Cannot reach server at " + host + ":" + port);
                loginButton.setDisable(false);
            });
            return;
        }

        client.send("LOGIN|" + username);
        String response;
        try {
            response = client.readLine();
        } catch (IOException e) {
            client.close();
            Platform.runLater(() -> {
                statusLabel.setText("Connection error: " + e.getMessage());
                loginButton.setDisable(false);
            });
            return;
        }

        if (response == null) {
            client.close();
            Platform.runLater(() -> {
                statusLabel.setText("Server closed the connection.");
                loginButton.setDisable(false);
            });
            return;
        }

        if (response.startsWith("LOGIN_OK")) {
            Platform.runLater(() -> openChat(client, username));
            return;
        }

        String reason = response.contains("|") ? response.substring(response.indexOf('|') + 1) : response;
        client.close();
        Platform.runLater(() -> {
            statusLabel.setText("Login failed: " + reason);
            loginButton.setDisable(false);
        });
    }

    private void openChat(ChatClient client, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/javafxdemo/chat-view.fxml"));
            Scene scene = new Scene(loader.load(), 820, 520);
            ChatController controller = loader.getController();
            controller.init(client, username);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Chat - " + username);
            stage.setOnCloseRequest(e -> {
                client.send("LOGOUT");
                client.close();
            });
        } catch (IOException e) {
            statusLabel.setText("Failed to open chat: " + e.getMessage());
            loginButton.setDisable(false);
        }
    }
}
