package com.auction.views;

import com.auction.controllers.BidController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * View class responsible for loading and displaying the bid screen.
 */
public class BidView {

  private Stage stage;

  private String auctionId;

  private String itemName;

  private String currentPrice;

  private String status;

  private String username;

  /**
   * Constructs a BidView with the given auction context.
   *
   * @param stage        the JavaFX stage to render into
   * @param auctionId    the ID of the auction
   * @param itemName     the name of the item being auctioned
   * @param currentPrice the current highest price as a display string
   * @param status       the auction status as a display string
   * @param username     the currently logged-in username
   */
  public BidView(Stage stage, String auctionId, String itemName,
      String currentPrice, String status, String username) {
    this.stage = stage;
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.currentPrice = currentPrice;
    this.status = status;
    this.username = username;
  }

  /**
   * Loads the BidView FXML and displays it on the stage.
   */
  public void show() {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/views/BidView.fxml"));
      Parent root = loader.load();

      BidController controller = loader.getController();
      String password = com.auction.util.SessionManager.getInstance().getPassword();
      controller.setData(auctionId, itemName, currentPrice, status, username, password);

      Scene scene = new Scene(root);
      stage.setTitle("Đấu giá - " + itemName);
      stage.setScene(scene);
      stage.show();
    } catch (Exception e) {
      System.err.println("Lỗi load FXML: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
