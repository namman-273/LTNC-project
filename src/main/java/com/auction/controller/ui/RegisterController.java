package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.views.java.LoginView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller màn đăng ký.
 */
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
      + "-fx-font-size: 12px; -fx-font-weight: bold;"
      + "-fx-background-radius: 10; -fx-cursor: hand;"
      + "-fx-border-color: #3B82F6; -fx-border-radius: 10; -fx-border-width: 2;";

  private static final String STYLE_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #94A3B8;"
      + "-fx-font-size: 12px; -fx-font-weight: bold;"
      + "-fx-background-radius: 10; -fx-cursor: hand;"
      + "-fx-border-color: #334155; -fx-border-radius: 10; -fx-border-width: 2;";

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    // Mặc định BIDDER được chọn
    setRoleStyle("BIDDER");
  }

  // ── Role selection ─────────────────────────────────────────────────────────
  @FXML
  private void handleRoleSelect(ActionEvent e) {
    if (e.getSource() == btnBidder) {
      selectedRole = "BIDDER";
    } else {
      selectedRole = "SELLER";
    }
    setRoleStyle(selectedRole); // DRY: không lặp 2 lần set style
  }

  /**
   * DRY: tập trung toàn bộ logic active/inactive style cho 2 nút role.
   */
  private void setRoleStyle(String activeRole) {
    btnBidder.setStyle("BIDDER".equals(activeRole) ? STYLE_ACTIVE : STYLE_INACTIVE);
    btnSeller.setStyle("SELLER".equals(activeRole) ? STYLE_ACTIVE : STYLE_INACTIVE);
  }

  // ── Register flow ──────────────────────────────────────────────────────────
  @FXML
  private void handleRegister() {
    String username = usernameField.getText().trim();
    String email = emailField != null ? emailField.getText().trim() : "";
    String password = passwordField.getText().trim();
    String confirmPassword = confirmPasswordField.getText().trim();

    // SRP: validation tách riêng
    String validationError = validateInputs(username, email, password, confirmPassword);
    if (validationError != null) {
      showMessage(validationError, false);
      return;
    }

    showMessage("Đang kết nối server...", null);

    new Thread(() -> {
      ServerConnection conn = ServerConnection.getInstance();
      if (!conn.connect()) {
        Platform.runLater(() -> showMessage("Không thể kết nối server!", false));
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
          showMessage("Mất kết nối server!", false);
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_REGISTER_SUCCESS)) {
          String msg = parts.length > 1 ? parts[1] : "Đăng ký thành công!";
          showMessage(msg + " Đang chuyển về đăng nhập...", true);
          redirectToLoginAfterDelay();
        } else {
          showMessage(parts.length > 1 ? parts[1] : "Đăng ký thất bại!", false);
        }
      });
    }, "register-thread").start();
  }

  @FXML
  private void handleBackToLogin() {
    Stage stage = (Stage) usernameField.getScene().getWindow();
    new LoginView(stage).show();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private String validateInputs(String username, String email,
      String password, String confirmPassword) {
    if (username.isEmpty())
      return "Vui lòng nhập tên đăng nhập!";
    if (username.length() < 3)
      return "Tên đăng nhập tối thiểu 3 ký tự!";
    if (password.isEmpty())
      return "Vui lòng nhập mật khẩu!";
    if (password.length() < 6)
      return "Mật khẩu tối thiểu 6 ký tự!";
    if (!password.equals(confirmPassword))
      return "Mật khẩu xác nhận không khớp!";
    if (!email.isEmpty() && !email.contains("@"))
      return "Email không hợp lệ!";
    return null;
  }

  private void showMessage(String msg, Boolean success) {
    if (success == null) {
      messageLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
    } else if (success) {
      messageLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 12px;");
    } else {
      messageLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 12px;");
    }
    messageLabel.setText(msg);
  }

  /** Delay 1 giây rồi chuyển về LoginView trên JavaFX thread. */
  private void redirectToLoginAfterDelay() {
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
    }, "redirect-login-thread").start();
  }
}