package com.example.javafxdemo;

import com.example.javafxdemo.chat.ChatServer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        ChatServer.startInBackground(ChatServer.DEFAULT_PORT);

        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/example/javafxdemo/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Chat Login");
        stage.setScene(scene);
        stage.show();
    }
}
