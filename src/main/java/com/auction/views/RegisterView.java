package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RegisterView {

    private Stage stage;

    public RegisterView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/auction/views/RegisterView.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setTitle("Đăng ký - Auction System");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}