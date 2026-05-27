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
 * Controller xử lý luồng đăng nhập.
 *
 *
 */
public class LoginController extends BaseController {

  @FXML private TextField     usernameField;
  @FXML private PasswordField passwordField;
  @FXML private Label         errorLabel;
  @FXML private Button        loginButton;

  // ── Handlers ──────────────────────────────────────────────────────────────

  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    setLoading(true);

    new Thread(() -> {
      ServerConnection.getInstance().disconnect();
      ServerConnection conn = ServerConnection.getInstance();

      // Dùng connectWithRetry — thử 3 lần tự động
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
          String role     = parts.length > 1 ? parts[1].trim() : "BIDDER";
          String greeting = parts.length > 2 ? parts[2].trim() : "";
          SessionManager.getInstance().setSession(username, password, role);
          showMessage(greeting, true);
          Stage stage = (Stage) usernameField.getScene().getWindow();
          new AuctionListView(stage, username).show();
        } else {
          String errorMsg = parts.length > 1 ? parts[1] : "Đăng nhập thất bại!";
          showMessage(errorMsg, false);
        }
      });
    }, "login-thread").start();
  }

  @FXML
  private void handleRegister() {
    Stage stage = (Stage) usernameField.getScene().getWindow();
    new RegisterView(stage).show();
  }

  // ── UI helpers ─────────────────────────────────────────────────────────────

  /**
   * Quản lý trạng thái loading — tách biệt với showMessage vì cần style "gray"
   * riêng cho label trong khi chờ kết nối (không phải error hay success).
   */
  private void setLoading(boolean loading) {
    usernameField.setDisable(loading);
    passwordField.setDisable(loading);
    if (loginButton != null) {
      loginButton.setDisable(loading);
      loginButton.setText(loading ? "Đang kết nối..." : "Đăng nhập");
    }
    if (loading) {
      errorLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
      errorLabel.setText("Đang kết nối đến server...");
    } else {
      errorLabel.setText("");
    }
  }

  /** SRP: một điểm duy nhất hiển thị kết quả thao tác. */
  private void showMessage(String msg, boolean success) {
    errorLabel.setStyle(success
        ? "-fx-text-fill: green; -fx-font-size: 12px;"
        : "-fx-text-fill: red;   -fx-font-size: 12px;");
    errorLabel.setText(msg);
  }
}