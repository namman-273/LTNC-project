package com.auction.views.java;

import com.auction.controller.ui.BidChartController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BidChartView {

    private Stage stage;
    private String auctionId;
    private String itemName;
    private String currentPrice;
    private String status;
    private String username;
    private long endTime;

    public BidChartView(Stage stage, String auctionId, String itemName,
                        String currentPrice, String status, String username, long endTime) {
        this.endTime = endTime;
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
                    getClass().getResource("/com/auction/views/fxml/BidChartView.fxml")
            );
            Parent root = loader.load();

            BidChartController controller = loader.getController();
            controller.setData(auctionId, itemName, currentPrice, status, username, endTime);

            Scene scene = new Scene(root);
            boolean wasMaximized = stage.isMaximized();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setTitle("Biểu đồ giá - " + itemName);
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
