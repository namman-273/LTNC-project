package com.auction.views.java;

import com.auction.controller.ui.BidHistoryController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BidHistoryView {
    private final Stage  stage;
    private final String username;

    public BidHistoryView(Stage stage, String username) {
        this.stage    = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/fxml/BidHistoryView.fxml"));
            Parent root = loader.load();
            BidHistoryController controller = loader.getController();
            controller.setUsername(username);
            stage.setTitle("Lịch sử đấu giá - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load BidHistoryView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
