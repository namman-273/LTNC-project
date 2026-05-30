package com.auction.controller.ui;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.SessionManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BidHistoryView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * .
 */
public class ProfileController extends BaseController implements Initializable {

  // ── Avatar card ───────────────────────────────────────────────────────────
  @FXML
  private Label avatarLabel;
  @FXML
  private Label usernameLabel;
  @FXML
  private Label roleLabel;
  @FXML
  private Label balanceLabel;

  // ── Thông tin cá nhân ─────────────────────────────────────────────────────
  @FXML
  private Label usernameFieldLabel;
  @FXML
  private TextField emailField;
  @FXML
  private Label roleDetailLabel;
  @FXML
  private Label joinDateLabel;

  // ── Panel đổi mật khẩu ────────────────────────────────────────────────────
  @FXML
  private VBox passwordPanel;
  @FXML
  private PasswordField oldPasswordField;
  @FXML
  private PasswordField newPasswordField;
  @FXML
  private PasswordField confirmPasswordField;

  // ── Thống kê ─────────────────────────────────────────────────────────────
  @FXML
  private Label statTotal;
  @FXML
  private Label statWin;
  @FXML
  private Label statWatchlist;
  @FXML
  private Label statRate;
  @FXML
  private Label messageLabel;

  private String username;

  /**
 * .
 */
  public void setUsername(String u) {
    initToastManager(avatarLabel); // BaseController — loại bỏ bản copy
    this.username = u;
    loadProfile();
    // Lắng nghe NOTI_BALANCE_CHANGED để cập nhật balance realtime
    registerPushListener(this::handlePushMessage); // BaseController
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    if (passwordPanel != null) {
      passwordPanel.setVisible(false);
      passwordPanel.setManaged(false);
    }
  }

  // ── Push ──────────────────────────────────────────────────────────────────
  private void handlePushMessage(String message) {
    String[] parts = message.split("\\|");
    if (parts.length >= 2 && Protocol.NOTI_BALANCE_CHANGED.equals(parts[0])) {
      Platform.runLater(() -> {
        // balanceLabel ở đây cần prefix "Số dư: "
        try {
          double b = Double.parseDouble(parts[1]);
          if (balanceLabel != null) {
            balanceLabel.setText(String.format("Số dư: %,.0f VNĐ", b));
          }
        } catch (NumberFormatException ignored) {
          ignored.printStackTrace();
        }
      });
    }
  }

  // ── Load toàn bộ profile ─────────────────────────────────────────────────
  private void loadProfile() {
    if (username == null || username.isEmpty()) {
      return;
    }

    String initials = username.substring(0, Math.min(2, username.length())).toUpperCase();
    avatarLabel.setText(initials);
    usernameLabel.setText(username);
    usernameFieldLabel.setText(username);

    applyRole(SessionManager.getInstance().getRole());
    refreshStats();

    new Thread(() -> {
      try {
        ServerConnection conn = ServerConnection.getInstance();

        String profRes = conn.sendAndReceive(Protocol.CMD_GET_PROFILE);
        if (profRes != null && profRes.startsWith(Protocol.RES_PROFILE_INFO)) {
          String[] p = profRes.split("\\|", -1);
          String email = p.length >= 3 ? p[2] : "";
          String balance = p.length >= 5 ? p[4] : "0";
          String joinDate = p.length >= 6 ? p[5] : "";
          Platform.runLater(() -> {
            if (emailField != null) {
              emailField.setText(email);
            }
            try {
              double b = Double.parseDouble(balance);
              balanceLabel.setText(String.format("Số dư: %,.0f VNĐ", b));
            } catch (NumberFormatException e) {
              balanceLabel.setText("Số dư: " + balance + " VNĐ");
            }
            if (joinDateLabel != null && !joinDate.isEmpty()) {
              joinDateLabel.setText(joinDate);
            }
          });
        }

        String wlRes = conn.sendAndReceive(Protocol.CMD_GET_WATCHLIST);
        if (wlRes != null && wlRes.startsWith(Protocol.RES_WATCHLIST)) {
          String[] p = wlRes.split("\\|", 2);
          String json = p.length >= 2 ? p[1] : "[]";
          long count = json.chars().filter(c -> c == '{').count();
          Platform.runLater(() -> {
            if (statWatchlist != null) {
              statWatchlist.setText(String.valueOf(count));
            }
          });
        }
      } catch (Exception e) {
        System.err.println("[ProfileController] Lỗi load: " + e.getMessage());
      }
    }, "profile-load-thread").start();
  }

  private void applyRole(String role) {
    if (role == null) {
      return;
    }
    roleLabel.setText(role);
    switch (role) {
      case "ADMIN" -> {
        roleDetailLabel.setText("Quản trị viên (Admin)");
        roleLabel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-background-radius: 12; -fx-padding: 3 10;");
      }
      case "SELLER" -> {
        roleDetailLabel.setText("Người bán (Seller)");
        roleLabel.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-background-radius: 12; -fx-padding: 3 10;");
      }
      default -> {
        roleDetailLabel.setText("Người đấu giá (Bidder)");
        roleLabel.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-background-radius: 12; -fx-padding: 3 10;");
      }
    }
  }

  private void refreshStats() {
    // ProfileController chạy phía client — BidHistoryManager là server-side singleton,
    // luôn rỗng trên client → thống kê luôn hiện 0/0/0%.
    // Fix: gọi CMD_GET_BID_HISTORY qua network, giống BidHistoryController.
    new Thread(() -> {
      try {
        String res = ServerConnection.getInstance()
                .sendAndReceive(Protocol.CMD_GET_BID_HISTORY);
        if (res == null || !res.startsWith(Protocol.RES_BID_HISTORY)) {
          return;
        }
        String json = res.substring(
                Protocol.RES_BID_HISTORY.length() + Protocol.SEPARATOR.length());
        com.google.gson.reflect.TypeToken<java.util.List<BidHistoryEntry>> token =
                new com.google.gson.reflect.TypeToken<java.util.List<BidHistoryEntry>>() {};
        java.util.List<BidHistoryEntry> entries =
                new com.google.gson.Gson().fromJson(json, token.getType());
        if (entries == null) {
          entries = new java.util.ArrayList<>();
        }
        long total = entries.size();
        long wins = entries.stream().filter(e -> "WIN".equalsIgnoreCase(e.getResult())).count();
        String rate = total > 0 ? String.format("%.0f%%", wins * 100.0 / total) : "0%";
        Platform.runLater(() -> {
          if (statTotal != null) { 
            statTotal.setText(String.valueOf(total)); 
          }
          if (statWin != null)  { 
            statWin.setText(String.valueOf(wins)); 
          }
          if (statRate != null) { 
            statRate.setText(rate); 
          }
        });
      } catch (Exception e) {
        System.err.println("[ProfileController] Lỗi load stats: " + e.getMessage());
      }
    }, "profile-stats-thread").start();
  }

  @FXML
  private void handleUpdateEmail() {
    String newEmail = emailField.getText().trim();
    if (newEmail.isEmpty()) {
      showMessage("Vui lòng nhập email!", false);
      return;
    }
    if (!newEmail.contains("@")) {
      showMessage("Email không hợp lệ!", false);
      return;
    }
    new Thread(() -> {
      String res = ServerConnection.getInstance()
              .sendAndReceive(Protocol.CMD_UPDATE_EMAIL + Protocol.SEPARATOR + newEmail);
      Platform.runLater(() -> {
        if (res != null && res.startsWith(Protocol.RES_SUCCESS)) {
          showMessage("✅ Cập nhật email thành công!", true);
        } else {
          showMessage("❌ " + (res != null ? res.replace("ERROR|", "") :
              "Không kết nối được server"), false);
        }
      });
    }, "update-email-thread").start();
  }

  @FXML
  private void handleChangePassword() {
    if (passwordPanel == null) {
      return;
    }
    boolean show = !passwordPanel.isVisible();
    passwordPanel.setVisible(show);
    passwordPanel.setManaged(show);
    if (!show) {
      if (oldPasswordField != null) {
        oldPasswordField.clear();
      }
      if (newPasswordField != null) {
        newPasswordField.clear();
      }
      if (confirmPasswordField != null) {
        confirmPasswordField.clear();
      }
      messageLabel.setText("");
    }
  }

  @FXML
  private void handleConfirmChangePassword() {
    if (oldPasswordField == null || newPasswordField == null || confirmPasswordField == null) {
      return;
    }
    String oldPass = oldPasswordField.getText().trim();
    String newPass = newPasswordField.getText().trim();
    String confirmPass = confirmPasswordField.getText().trim();

    if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
      showMessage("Vui lòng điền đủ các trường mật khẩu!", false);
      return;
    }
    if (!newPass.equals(confirmPass)) {
      showMessage("Mật khẩu xác nhận không khớp!", false);
      return;
    }
    if (newPass.length() < 6) {
      showMessage("Mật khẩu mới phải ít nhất 6 ký tự!", false);
      return;
    }
    if (newPass.equals(oldPass)) {
      showMessage("Mật khẩu mới phải khác mật khẩu cũ!", false);
      return;
    }

    new Thread(() -> {
      String res = ServerConnection.getInstance().sendAndReceive(
              Protocol.CMD_UPDATE_PASSWORD + Protocol.SEPARATOR 
             + oldPass + Protocol.SEPARATOR + newPass);
      Platform.runLater(() -> {
        if (res != null && res.startsWith(Protocol.RES_SUCCESS)) {
          showMessage("✅ Đổi mật khẩu thành công!", true);
          passwordPanel.setVisible(false);
          passwordPanel.setManaged(false);
          oldPasswordField.clear();
          newPasswordField.clear();
          confirmPasswordField.clear();
        } else {
          showMessage("❌ " + (res != null ? res.replace("ERROR|", "") : 
              "Lỗi kết nối server"), false);
        }
      });
    }, "change-password-thread").start();
  }

  @FXML
  private void handleViewHistory() {
    Stage stage = (Stage) usernameLabel.getScene().getWindow();
    new BidHistoryView(stage, username).show();
  }

  @FXML
  private void handleBack() {
    removePushListener(); // BaseController
    Stage stage = (Stage) usernameLabel.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  private void showMessage(String msg, boolean success) {
    if (messageLabel == null) {
      return;
    }
    messageLabel.setText(msg);
    messageLabel.setStyle(success
            ? "-fx-font-size: 12px; -fx-text-fill: #22C55E;"
            : "-fx-font-size: 12px; -fx-text-fill: #EF4444;");
  }
}