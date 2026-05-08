package com.auction;

import javafx.application.Application;
import javafx.stage.Stage;
import com.auction.views.LoginView;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginView loginView = new LoginView(primaryStage);
        loginView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}