package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * View class responsible for loading and displaying the login screen.
 */
public class LoginView {

  private Stage stage;

  /**
   * Constructs a LoginView for the given stage.
   *
   * @param stage the JavaFX stage to render into
   */
  public LoginView(Stage stage) {
    this.stage = stage;
  }

  /**
   * Loads the LoginView FXML and displays it on the stage.
   */
  public void show() {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/views/LoginView.fxml"));
      Parent root = loader.load();
      Scene scene = new Scene(root);
      stage.setTitle("Đăng nhập - Auction System");
      stage.setScene(scene);
      stage.show();
    } catch (Exception e) {
      System.err.println("Lỗi load FXML: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
