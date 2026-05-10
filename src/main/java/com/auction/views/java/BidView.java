package com.auction.views.java;

import com.auction.controller.ui.BidController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BidView {

  private final Stage stage;
  private final String auctionId;
  private final String itemName;
  private final String currentPrice;
  private final String status;
  private final String username;
  private final long endTime;
  private final String imageUrl;
  private final String description;

  public BidView(Stage stage, String auctionId, String itemName,
                 String currentPrice, String status, String username,
                 long endTime) {
    this(stage, auctionId, itemName, currentPrice, status, username, endTime, "", "");
  }

  public BidView(Stage stage, String auctionId, String itemName,
                 String currentPrice, String status, String username,
                 long endTime, String imageUrl, String description) {
    this.stage        = stage;
    this.auctionId    = auctionId;
    this.itemName     = itemName;
    this.currentPrice = currentPrice;
    this.status       = status;
    this.username     = username;
    this.endTime      = endTime;
    this.imageUrl     = imageUrl != null ? imageUrl : "";
    this.description  = description != null ? description : "";
  }

  public void show() {
    try {
      FXMLLoader loader = new FXMLLoader(
              getClass().getResource("/com/auction/views/fxml/BidView.fxml"));
      Parent root = loader.load();

      BidController controller = loader.getController();
      controller.setData(auctionId, itemName, currentPrice, status,
              username, endTime);

      stage.setTitle("Đấu giá - " + itemName);
      stage.setScene(new Scene(root));
      stage.show();
    } catch (Exception e) {
      System.err.println("Lỗi load BidView: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
