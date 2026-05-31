package com.auction.views.java;

import com.auction.controller.ui.AuctionListController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * .
 */
public class AuctionListView {

  private Stage stage;
  private String username;
  private String successToastMessage;

  public AuctionListView(Stage stage, String username) {
    this.stage = stage;
    this.username = username;
  }

  public AuctionListView(Stage stage, String username, String successToastMessage) {
    this.stage = stage;
    this.username = username;
    this.successToastMessage = successToastMessage;
  }

  /**
   * .
   */
  public void show() {
    try {
      FXMLLoader loader = new FXMLLoader(
              getClass().getResource("/com/auction/views/fxml/AuctionListView.fxml"));
      Parent root = loader.load();

      AuctionListController controller = loader.getController();
      controller.setUsername(username);

      double prevW = stage.getScene() != null ? stage.getScene().getWidth() : 0;
      double prevH = stage.getScene() != null ? stage.getScene().getHeight() : 0;
      stage.setTitle("Danh sách phiên - Auction System");
      stage.setScene(prevW > 100 ? new Scene(root, prevW, prevH) : new Scene(root));
      stage.show();
      boolean wasMaximized = stage.isMaximized();
      if (wasMaximized) {
        stage.setMaximized(true);
      }
      // Bind root tự stretch theo kích thước scene
      if (root instanceof javafx.scene.layout.Region) {
        javafx.scene.layout.Region regionRoot = (javafx.scene.layout.Region) root;
        regionRoot.prefWidthProperty().bind(stage.getScene().widthProperty());
        regionRoot.prefHeightProperty().bind(stage.getScene().heightProperty());
      }

      // Load lại sau 500ms để đảm bảo socket sẵn sàng
      final String toastMsg = successToastMessage;
      new Thread(() -> {
        try {
          Thread.sleep(800);
          javafx.application.Platform.runLater(() -> {
            controller.refreshList();
            if (toastMsg != null && !toastMsg.isEmpty()) {
              com.auction.util.ui.ToastManager.show(
                      com.auction.util.ui.ToastManager.Type.SUCCESS, toastMsg);
            }
          });
        } catch (InterruptedException ignored) {
        }
      }).start();

    } catch (Exception e) {
      System.err.println("Lỗi load FXML: " + e.getMessage());
      e.printStackTrace();
    }
  }
}