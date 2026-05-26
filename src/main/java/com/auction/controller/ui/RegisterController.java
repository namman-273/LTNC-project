package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.views.java.LoginView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController implements Initializable {

  @FXML
  private TextField usernameField;
  @FXML
  private TextField emailField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private PasswordField confirmPasswordField;
  @FXML
  private Button btnBidder;
  @FXML
  private Button btnSeller;
  @FXML
  private Label messageLabel;

  private String selectedRole = "BIDDER";

  private static final String STYLE_ACTIVE = "-fx-background-color: #1D4ED8; -fx-text-fill: white;" 
      +
      "-fx-font-size: 12px; -fx-font-weight: bold;" 
      +
      "-fx-background-radius: 10; -fx-cursor: hand;" 
      +
      "-fx-border-color: #3B82F6; -fx-border-radius: 10; -fx-border-width: 2;";

  private static final String STYLE_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #94A3B8;" 
      +
      "-fx-font-size: 12px; -fx-font-weight: bold;" 
      +
      "-fx-background-radius: 10; -fx-cursor: hand;" 
      +
      "-fx-border-color: #334155; -fx-border-radius: 10; -fx-border-width: 2;";

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    btnBidder.setStyle(STYLE_ACTIVE);
    btnSeller.setStyle(STYLE_INACTIVE);
  }

  @FXML
  private void handleRoleSelect(javafx.event.ActionEvent e) {
    if (e.getSource() == btnBidder) {
      selectedRole = "BIDDER";
      btnBidder.setStyle(STYLE_ACTIVE);
      btnSeller.setStyle(STYLE_INACTIVE);
    } else {
      selectedRole = "SELLER";
      btnSeller.setStyle(STYLE_ACTIVE);
      btnBidder.setStyle(STYLE_INACTIVE);
    }
  }

  @FXML
  private void handleRegister() {
    String username = usernameField.getText().trim();
    String email = emailField != null ? emailField.getText().trim() : "";
    String password = passwordField.getText().trim();
    String confirmPassword = confirmPasswordField.getText().trim();

    if (!password.equals(confirmPassword)) {
      showError("Mật khẩu xác nhận không khớp!");
      return;
    }

    showSuccess("Đang kết nối server...");

    new Thread(() -> {
      ServerConnection conn = ServerConnection.getInstance();
      if (!conn.connect()) {
        Platform.runLater(() -> showError("Không thể kết nối server!"));
        return;
      }

      String response = conn.sendAndReceive(
          Protocol.CMD_REGISTER + Protocol.SEPARATOR
              + username + Protocol.SEPARATOR
              + password + Protocol.SEPARATOR
              + selectedRole + Protocol.SEPARATOR
              + email);

      Platform.runLater(() -> {
        if (response == null) {
          showError("Mất kết nối server!");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_REGISTER_SUCCESS)) {
          String msg = parts.length > 1 ? parts[1] : "Đăng ký thành công!";
          showSuccess(msg + " Đang chuyển về đăng nhập...");
          new Thread(() -> {
            try {
              Thread.sleep(1000);
              Platform.runLater(() -> {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                new LoginView(stage).show();
              });
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          }).start();
        } else {
          showError(parts.length > 1 ? parts[1] : "Đăng ký thất bại!");
        }
      });
    }).start();
  }

  @FXML
  private void handleBackToLogin() {
    Stage stage = (Stage) usernameField.getScene().getWindow();
    new LoginView(stage).show();
  }

  private void showError(String msg) {
    messageLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 12px;");
    messageLabel.setText(msg);
  }

  private void showSuccess(String msg) {
    messageLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 12px;");
    messageLabel.setText(msg);
  }
}