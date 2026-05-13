package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.BidHistoryManager;
import com.auction.util.core.SessionManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BidHistoryView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label    avatarLabel;
    @FXML private Label    usernameLabel;
    @FXML private Label    usernameFieldLabel;
    @FXML private Label    roleLabel;
    @FXML private Label    roleDetailLabel;
    @FXML private Label    balanceLabel;
    @FXML private TextField emailField;
    @FXML private Label    messageLabel;
    @FXML private Label    statTotal;
    @FXML private Label    statWin;
    @FXML private Label    statWatchlist;
    @FXML private Label    statRate;

    private String username;

    public void setUsername(String u) {
        this.username = u;
        loadProfile();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    private void loadProfile() {
        // Avatar initials
        if (username != null && !username.isEmpty()) {
            String initials = username.substring(0, Math.min(2, username.length())).toUpperCase();
            avatarLabel.setText(initials);
            usernameLabel.setText(username);
            usernameFieldLabel.setText(username);
        }

        // Role từ SessionManager
        String role = SessionManager.getInstance().getRole();
        if (role != null) {
            roleLabel.setText(role);
            switch (role) {
                case "ADMIN":
                    roleDetailLabel.setText("Quản trị viên (Admin)");
                    roleLabel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 12; -fx-padding: 3 10;");
                    break;
                case "SELLER":
                    roleDetailLabel.setText("Người bán (Seller)");
                    roleLabel.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 12; -fx-padding: 3 10;");
                    break;
                default:
                    roleDetailLabel.setText("Người đấu giá (Bidder)");
                    roleLabel.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 12; -fx-padding: 3 10;");
            }
        }

        // Thống kê từ BidHistoryManager
        BidHistoryManager m = BidHistoryManager.getInstance();
        statTotal.setText(String.valueOf(m.totalCount()));
        statWin.setText(String.valueOf(m.winCount()));
        statRate.setText(m.winRate());

        // Load balance + email từ server
        new Thread(() -> {
            try {
                // Lấy balance
                String balRes = ServerConnection.getInstance()
                        .sendAndReceive(Protocol.CMD_GET_BALANCE);
                if (balRes != null && balRes.startsWith(Protocol.RES_BALANCE_INFO)) {
                    String[] parts = balRes.split("\\|");
                    String bal = parts.length >= 2 ? parts[1] : "0";
                    Platform.runLater(() -> {
                        try {
                            double b = Double.parseDouble(bal);
                            balanceLabel.setText(String.format("Số dư: %,.0f VNĐ", b));
                        } catch (Exception e) {
                            balanceLabel.setText("Số dư: " + bal + " VNĐ");
                        }
                    });
                }

                // Lấy profile (email) nếu BE đã có
                String profRes = ServerConnection.getInstance()
                        .sendAndReceive(Protocol.CMD_GET_PROFILE);
                if (profRes != null && profRes.startsWith(Protocol.RES_PROFILE_INFO)) {
                    String[] parts = profRes.split("\\|");
                    // PROFILE_INFO|username|email|role|balance
                    String email = parts.length >= 3 ? parts[2] : "";
                    Platform.runLater(() -> emailField.setText(email));
                }

                // Watchlist count
                String wlRes = ServerConnection.getInstance()
                        .sendAndReceive(Protocol.CMD_GET_WATCHLIST);
                if (wlRes != null && wlRes.startsWith(Protocol.RES_WATCHLIST)) {
                    String[] parts = wlRes.split("\\|");
                    String json = parts.length >= 2 ? parts[1] : "[]";
                    long count = json.chars().filter(c -> c == '{').count();
                    Platform.runLater(() -> statWatchlist.setText(String.valueOf(count)));
                }

            } catch (Exception e) {
                System.err.println("Lỗi load profile: " + e.getMessage());
            }
        }, "profile-load-thread").start();
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
                if (res != null && res.startsWith("SUCCESS")) {
                    showMessage("Cập nhật email thành công!", true);
                } else {
                    showMessage("Lỗi: " + (res != null ? res : "Không kết nối được server"), false);
                }
            });
        }, "update-email-thread").start();
    }

    @FXML
    private void handleChangePassword() {
        // Chờ BE hoàn thiện CMD_CHANGE_PASSWORD
        // Tạm hiện dialog đơn giản
        showMessage("Tính năng đổi mật khẩu đang phát triển.", false);
    }

    @FXML
    private void handleViewHistory() {
        Stage stage = (Stage) usernameLabel.getScene().getWindow();
        new BidHistoryView(stage, username).show();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) usernameLabel.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private void showMessage(String msg, boolean success) {
        messageLabel.setText(msg);
        messageLabel.setStyle(success
                ? "-fx-font-size: 12px; -fx-text-fill: #22C55E;"
                : "-fx-font-size: 12px; -fx-text-fill: #EF4444;");
    }
}