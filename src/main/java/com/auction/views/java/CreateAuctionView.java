package com.auction.views.java;

import com.auction.controller.ui.CreateAuctionController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CreateAuctionView {

    private Stage stage;
    private String username;

    public CreateAuctionView(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/fxml/CreateAuctionView.fxml")
            );
            Parent root = loader.load();

            CreateAuctionController controller = loader.getController();
            controller.setUsername(username);

            Scene scene = new Scene(root);
            boolean wasMaximized = stage.isMaximized();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setTitle("Tạo phiên đấu giá");
            stage.setScene(scene);
            stage.show();
            if (wasMaximized) {
                stage.setMaximized(true);
            } else {
                if (!Double.isNaN(w) && w > 100) stage.setWidth(w);
                if (!Double.isNaN(h) && h > 100) stage.setHeight(h);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
