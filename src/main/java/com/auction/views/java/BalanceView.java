package com.auction.views.java;

import com.auction.controller.ui.BalanceController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BalanceView {

    private final Stage stage;
    private final String username;

    public BalanceView(Stage stage, String username) {
        this.stage    = stage;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/BalanceView.fxml")
            );
            Parent root = loader.load();

            BalanceController controller = loader.getController();
            controller.setUsername(username);

            stage.setTitle("Số dư tài khoản - 1388AUCTION");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load BalanceView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}