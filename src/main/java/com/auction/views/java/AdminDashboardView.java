package com.auction.views.java;

import com.auction.controller.ui.AdminDashboardController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
              getClass().getResource("/com/auction/views/fxml/AdminDashboardView.fxml"));
      Parent root = loader.load();

      AdminDashboardController controller = loader.getController();
      controller.setUsername(username);

      Scene scene = new Scene(root);
      boolean wasMaximized = stage.isMaximized();
      double w = stage.getWidth();
      double h = stage.getHeight();
      stage.setTitle("Admin Dashboard - Auction System");
      stage.setScene(scene);
      stage.show();
      if (wasMaximized) {
        stage.setMaximized(true);
      } else {
        if (!Double.isNaN(w) && w > 100) stage.setWidth(w);
        if (!Double.isNaN(h) && h > 100) stage.setHeight(h);
      }

    } catch (Exception e) {
      System.err.println("Lỗi load AdminDashboard: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
