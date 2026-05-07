package com.auction.views;

import com.auction.controllers.WatchlistController;
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
                    getClass().getResource("/com/auction/views/WatchlistView.fxml")
            );
            Parent root = loader.load();

            WatchlistController controller = loader.getController();
            controller.setUsername(username);

            stage.setTitle("Watchlist - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load WatchlistView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
