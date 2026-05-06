package com.auction.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.RegisterView;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton; // Thêm fx:id="loginButton" vào FXML nếu chưa có

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Disable UI ngay lập tức trên JavaFX thread trước khi chuyển sang background
        setLoading(true);

        // Toàn bộ network chạy trên background thread — không bao giờ block UI thread
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();

            // Reset connection cũ nếu còn sót lại
            if (conn.isConnected()) {
                conn.disconnect();
            }
            conn = ServerConnection.getInstance();

            if (!conn.connect()) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Không thể kết nối server! Vui lòng thử lại.");
                });
                return;
            }

            String response = conn.sendAndReceive("LOGIN|" + username + "|" + password);
            System.out.println("Server trả về: " + response);

            // Chụp lại conn để dùng trong lambda (phải là effectively final)
            final ServerConnection finalConn = conn;

            Platform.runLater(() -> {
                setLoading(false);

                if (response == null) {
                    showError("Mất kết nối server!");
                    return;
                }

                if (response.startsWith("LOGIN_SUCCESS")) {
                    String[] parts = response.split("\\|");
                    String role = parts.length > 1 ? parts[1].trim() : "BIDDER";

                    // Lưu session — có password để listener dùng lại, tránh login nhiều lần
                    SessionManager.getInstance().setSession(username, password, role);

                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    new AuctionListView(stage, username).show();
                } else {
                    showError("Sai tên đăng nhập hoặc mật khẩu!");
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        new RegisterView(stage).show();
    }

    /**
     * Bật/tắt trạng thái loading.
     * Disable input + hiện thông báo để user biết app đang xử lý, không bị treo.
     * Phải gọi trên JavaFX thread (gọi trực tiếp hoặc trong Platform.runLater).
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
            errorLabel.setText(""); // Xoá thông báo loading, để showError() tự điền nếu lỗi
        }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setText(msg);
    }
}