package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.auction.controllers.SellerController;

public class SellerView {

    private Stage stage;
    private String username;

    public SellerView(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/SellerView.fxml")
            );
            Parent root = loader.load();
            stage.setTitle("Seller Dashboard - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load SellerView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}