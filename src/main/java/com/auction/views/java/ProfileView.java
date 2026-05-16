package com.auction.views.java;

import com.auction.controller.ui.ProfileController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProfileView {
    private final Stage  stage;
    private final String username;

    public ProfileView(Stage stage, String username) {
        this.stage    = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/fxml/ProfileView.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.setUsername(username);
            boolean wasMaximized = stage.isMaximized();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setTitle("Hồ sơ cá nhân - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
            if (wasMaximized) {
                stage.setMaximized(true);
            } else {
                if (!Double.isNaN(w) && w > 100) stage.setWidth(w);
                if (!Double.isNaN(h) && h > 100) stage.setHeight(h);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load ProfileView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
