package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.SessionManager;
import com.auction.util.ui.AlertUtil;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.RegisterView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller màn đăng nhập.
 */
public class LoginController {

  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Label errorLabel;
  @FXML
  private Button loginButton;

  // ── Login flow ─────────────────────────────────────────────────────────────
  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    String validationError = validateInput(username, password);
    if (validationError != null) {
      showMessage(validationError, false);
      return;
    }

    setLoading(true);

    new Thread(() -> {
      // Ngắt kết nối cũ trước khi thử lại — tránh dùng socket đã chết
      ServerConnection.getInstance().disconnect();
      ServerConnection conn = ServerConnection.getInstance();

      if (!conn.connectWithRetry()) {
        Platform.runLater(() -> {
          setLoading(false);
          showMessage("Không thể kết nối server sau nhiều lần thử!", false);
          AlertUtil.showError("Mất kết nối",
              "Không thể kết nối đến server!\nVui lòng kiểm tra server đang chạy chưa.");
        });
        return;
      }

      String response = conn.sendAndReceive(
          Protocol.CMD_LOGIN + Protocol.SEPARATOR + username + Protocol.SEPARATOR + password);

      Platform.runLater(() -> {
        setLoading(false);

        if (response == null || response.startsWith("ERROR|Mất kết nối")) {
          showMessage("Mất kết nối server!", false);
          AlertUtil.showError("Mất kết nối", "Mất kết nối khi đăng nhập. Vui lòng thử lại.");
          return;
        }

        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_LOGIN_SUCCESS)) {
          String role = parts.length > 1 ? parts[1].trim() : "BIDDER";
          String greeting = parts.length > 2 ? parts[2].trim() : "";
          SessionManager.getInstance().setSession(username, password, role);
          showMessage(greeting, true);
          Stage stage = (Stage) usernameField.getScene().getWindow();
          new AuctionListView(stage, username).show();
        } else {
          showMessage(parts.length > 1 ? parts[1] : "Đăng nhập thất bại!", false);
        }
      });
    }, "login-thread").start();
  }

  @FXML
  private void handleRegister() {
    Stage stage = (Stage) usernameField.getScene().getWindow();
    new RegisterView(stage).show();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  /**
   * SRP: chỉ kiểm tra input, không làm việc khác.
   *
   * @return chuỗi lỗi nếu không hợp lệ, null nếu OK.
   */
  private String validateInput(String username, String password) {
    if (username.isEmpty()) {
      return "Vui lòng nhập tên đăng nhập!";
    }
    if (password.isEmpty()) {
      return "Vui lòng nhập mật khẩu!";
    }
    return null;
  }

  /**
   * SRP: chỉ quản lý trạng thái loading của UI.
   */
  private void setLoading(boolean loading) {
    usernameField.setDisable(loading);
    passwordField.setDisable(loading);
    if (loginButton != null) {
      loginButton.setDisable(loading);
      loginButton.setText(loading ? "Đang kết nối..." : "Đăng nhập");
    }
    if (loading) {
      showMessage("Đang kết nối đến server...", null); // null = gray
    } else {
      errorLabel.setText("");
    }
  }

  private void showMessage(String msg, Boolean success) {
    if (success == null) {
      errorLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
    } else if (success) {
      errorLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
    } else {
      errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }
    errorLabel.setText(msg);
  }
}