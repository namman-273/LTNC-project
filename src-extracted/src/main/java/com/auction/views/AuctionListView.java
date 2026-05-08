package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.auction.controllers.AuctionListController;

public class AuctionListView {

    private Stage stage;
    private String username;

    public AuctionListView(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/AuctionListView.fxml")
            );
            Parent root = loader.load();

            AuctionListController controller = loader.getController();
            controller.setUsername(username);

            Scene scene = new Scene(root);
            stage.setTitle("Danh sách phiên - Auction System");
            stage.setScene(scene);
            stage.show();

            // Load lại sau 500ms để đảm bảo socket sẵn sàng
            new Thread(() -> {
                try {
                    Thread.sleep(800);
                    javafx.application.Platform.runLater(() -> controller.refreshList());
                } catch (InterruptedException ignored) {}
            }).start();

        } catch (Exception e) {
            System.err.println("Lỗi load FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}