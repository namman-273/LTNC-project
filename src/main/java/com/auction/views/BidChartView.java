package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.auction.controllers.BidChartController;

public class BidChartView {

    private Stage stage;
    private String auctionId;
    private String itemName;
    private String currentPrice;
    private String status;
    private String username;

    public BidChartView(Stage stage, String auctionId, String itemName,
                        String currentPrice, String status, String username) {
        this.stage = stage;
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/views/BidChartView.fxml")
            );
            Parent root = loader.load();

            BidChartController controller = loader.getController();
            controller.setData(auctionId, itemName, currentPrice, status, username);

            Scene scene = new Scene(root);
            stage.setTitle("Biểu đồ giá - " + itemName);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}