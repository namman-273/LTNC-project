package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.auction.controllers.AdminDashboardController;

public class AdminDashboardView {

  private Stage stage;
  private String username;

  public AdminDashboardView(Stage stage, String username) {
    this.stage = stage;
    this.username = username;
  }

  public void show() {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/views/AdminDashboardView.fxml"));
      Parent root = loader.load();

      AdminDashboardController controller = loader.getController();
      controller.setUsername(username);

      Scene scene = new Scene(root);
      stage.setTitle("Admin Dashboard - Auction System");
      stage.setScene(scene);
      stage.show();

    } catch (Exception e) {
      System.err.println("Lỗi load AdminDashboard: " + e.getMessage());
      e.printStackTrace();
    }
  }
}