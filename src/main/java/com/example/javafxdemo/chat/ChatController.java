package com.example.javafxdemo.chat;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.Map;

public class ChatController {

    @FXML
    private Label meLabel;
    @FXML
    private ListView<String> userList;
    @FXML
    private Label chatWithLabel;
    @FXML
    private ListView<String> messageList;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;

    private ChatClient client;
    private String me;
    private String selectedUser;
    private final Map<String, ObservableList<String>> history = new HashMap<>();
    private final ObservableList<String> emptyList = FXCollections.observableArrayList();

    public void init(ChatClient client, String me) {
        this.client = client;
        this.me = me;
        meLabel.setText("Logged in as: " + me);
        messageList.setItems(emptyList);
        inputField.setDisable(true);
        sendButton.setDisable(true);
        chatWithLabel.setText("Select a user to chat");

        userList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectUser(newV);
            }
        });

        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        client.startListening(this::onServerMessage);
    }

    private void selectUser(String user) {
        selectedUser = user;
        chatWithLabel.setText("Chat with " + user);
        messageList.setItems(history.computeIfAbsent(user, k -> FXCollections.observableArrayList()));
        inputField.setDisable(false);
        sendButton.setDisable(false);
        inputField.requestFocus();
    }

    private void sendMessage() {
        if (selectedUser == null) {
            return;
        }
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        client.send("MSG|" + me + "|" + selectedUser + "|" + text);
        appendMessage(selectedUser, "You: " + text);
        inputField.clear();
    }

    private void onServerMessage(String line) {
        System.out.println("Message from Server: " + line);
        Platform.runLater(() -> handleServerMessage(line));
    }

    private void handleServerMessage(String line) {
        String[] parts = line.split("\\|", 4);
        switch (parts[0]) {
            case "USERS" -> updateUserList(parts.length > 1 ? parts[1] : "");
            case "MSG" -> {
                if (parts.length >= 4) {
                    receiveMessage(parts[1], parts[3]);
                }
            }
            case "DISCONNECTED" -> {
                chatWithLabel.setText("Disconnected from server");
                inputField.setDisable(true);
                sendButton.setDisable(true);
            }
            default -> {
            }
        }
    }

    private void updateUserList(String csv) {
        ObservableList<String> list = FXCollections.observableArrayList();
        if (!csv.isEmpty()) {
            for (String u : csv.split(",")) {
                if (!u.isEmpty() && !u.equals(me)) {
                    list.add(u);
                }
            }
        }
        String prevSelection = selectedUser;
        userList.setItems(list);
        if (prevSelection != null && list.contains(prevSelection)) {
            userList.getSelectionModel().select(prevSelection);
        }
    }

    private void receiveMessage(String from, String text) {
        appendMessage(from, from + ": " + text);
        if (selectedUser == null) {
            userList.getSelectionModel().select(from);
        }
    }

    private void appendMessage(String user, String display) {
        ObservableList<String> list = history.computeIfAbsent(user, k -> FXCollections.observableArrayList());
        list.add(display);
        if (user.equals(selectedUser)) {
            messageList.scrollTo(list.size() - 1);
        }
    }
}
