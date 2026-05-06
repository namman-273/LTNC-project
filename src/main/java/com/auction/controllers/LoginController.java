package com.auction.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.exception.AuthenticationException;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.RegisterView;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validate payload đúng như BE: LOGIN cần đủ 3 parts (CMD|user|pass)
        // Nếu thiếu username/password thì không đủ payload → báo lỗi ngay
        String[] payload = {Protocol.CMD_LOGIN, username, password};
        for (String part : payload) {
            if (part == null || part.isEmpty()) {
                showError("Vui lòng nhập đầy đủ thông tin!");
                return;
            }
        }

        setLoading(true);

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
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

            // Dùng Protocol.CMD_LOGIN + SEPARATOR — đúng format BE expect
            String request = Protocol.CMD_LOGIN
                    + Protocol.SEPARATOR + username
                    + Protocol.SEPARATOR + password;
            String response = conn.sendAndReceive(request);
            System.out.println("Server trả về: " + response);

            Platform.runLater(() -> {
                setLoading(false);

                if (response == null) {
                    showError("Mất kết nối server!");
                    return;
                }

                String[] parts = response.split("\\" + Protocol.SEPARATOR);

                if (response.startsWith(Protocol.RES_LOGIN_SUCCESS)) {
                    // BE trả: LOGIN_SUCCESS|ROLE|Chào username
                    // parts[1] = role, parts[2] = lời chào từ BE
                    String role     = parts.length > 1 ? parts[1].trim() : "BIDDER";
                    String greeting = parts.length > 2 ? parts[2].trim() : "";

                    // Lưu session qua SessionManager của BE
                    SessionManager.getInstance().setSession(username, password, role);

                    // Hiển thị lời chào từ BE trước khi chuyển màn
                    showSuccess(greeting);

                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    new AuctionListView(stage, username).show();

                } else if (response.startsWith(Protocol.RES_LOGIN_FAILED)) {
                    // BE trả: LOGIN_FAILED|message từ AuthenticationException
                    // Lấy đúng message BE throw ra, không tự viết lại
                    String errorMsg = parts.length > 1
                            ? parts[1]
                            : "Đăng nhập thất bại!";
                    showError(errorMsg);

                } else if (response.startsWith(Protocol.ERROR)) {
                    // BE trả: ERROR|Lệnh không hợp lệ hoặc thiếu tham số
                    String errorMsg = parts.length > 1
                            ? parts[1]
                            : "Lỗi không xác định!";
                    showError(errorMsg);
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        new RegisterView(stage).show();
    }

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

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        errorLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        errorLabel.setText(msg);
    }
}