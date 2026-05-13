package com.auction.views.java;

import com.auction.controller.ui.NotificationController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NotificationView {

    private final Stage stage;
    private final String username;

    public NotificationView(Stage stage, String username) {
        this.stage    = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/fxml/NotificationView.fxml"));
            Parent root = loader.load();

            NotificationController controller = loader.getController();
            controller.setUsername(username);

            stage.setTitle("Thông báo - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load NotificationView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}