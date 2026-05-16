package com.auction.views.java;

import com.auction.controller.ui.WatchlistController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WatchlistView {

    private final Stage stage;
    private final String username;

    public WatchlistView(Stage stage, String username) {
        this.stage    = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/fxml/WatchlistView.fxml")
            );
            Parent root = loader.load();

            WatchlistController controller = loader.getController();
            controller.setUsername(username);

            boolean wasMaximized = stage.isMaximized();
            stage.setTitle("Danh sách theo dõi - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load WatchlistView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
