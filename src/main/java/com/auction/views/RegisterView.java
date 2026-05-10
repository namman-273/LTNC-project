package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * View class responsible for loading and displaying the registration screen.
 */
public class RegisterView {

  private Stage stage;

  /**
   * Constructs a RegisterView for the given stage.
   *
   * @param stage the JavaFX stage to render into
   */
  public RegisterView(Stage stage) {
    this.stage = stage;
  }

  /**
   * Loads the RegisterView FXML and displays it on the stage.
   */
  public void show() {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/views/RegisterView.fxml"));
      Parent root = loader.load();
      Scene scene = new Scene(root);
      stage.setTitle("Đăng ký - Auction System");
      stage.setScene(scene);
      stage.show();
    } catch (Exception e) {
      System.err.println("Lỗi load FXML: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
