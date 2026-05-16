package com.auction.views.java;

import com.auction.controller.ui.SellerController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SellerView {

    private Stage stage;
    private String username;

    public SellerView(Stage stage, String username) {
        this.stage    = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/fxml/SellerView.fxml")
            );
            Parent root = loader.load();

            // Fix: lấy controller ra set username như AdminDashboardView
            SellerController controller = loader.getController();
            controller.setUsername(username);

            boolean wasMaximized = stage.isMaximized();
            stage.setTitle("Seller Dashboard - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load SellerView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
