package com.auction.views.java;

import com.auction.controller.ui.AutoBidController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AutoBidView {

    private final Stage stage;
    private final String auctionId;
    private final String itemName;
    private final String currentPrice;
    private final String status;
    private final String username;
    private final long endTime;

    public AutoBidView(Stage stage, String auctionId, String itemName,
                       String currentPrice, String status, String username, long endTime) {
        this.stage        = stage;
        this.auctionId    = auctionId;
        this.itemName     = itemName;
        this.currentPrice = currentPrice;
        this.status       = status;
        this.username     = username;
        this.endTime      = endTime;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/AutoBidView.fxml")
            );
            Parent root = loader.load();

            AutoBidController controller = loader.getController();
            controller.setData(auctionId, itemName, currentPrice, status, username, endTime);

            stage.setTitle("Auto-Bid - " + itemName);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load AutoBidView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}