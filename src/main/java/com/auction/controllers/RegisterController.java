package com.auction.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.views.LoginView;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label messageLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleComboBox.setItems(FXCollections.observableArrayList("BIDDER", "SELLER"));
        roleComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String role = roleComboBox.getValue();

        // Kiểm tra đầu vào
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        if (role == null) {
            showError("Vui lòng chọn vai trò!");
            return;
        }

        // Gửi lệnh REGISTER lên server
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.connect()) {
            showError("Không thể kết nối server!");
            return;
        }

        String response = conn.sendAndReceive("REGISTER|" + username + "|" + password + "|" + role);
        System.out.println("Server tra ve: " + response);

        if (response != null && response.startsWith("REGISTER_SUCCESS")) {
            showSuccess("Đăng ký thành công! Đang chuyển về đăng nhập...");
            // Chờ 1 giây rồi chuyển về Login
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> {
                        Stage stage = (Stage) usernameField.getScene().getWindow();
                        LoginView loginView = new LoginView(stage);
                        loginView.show();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Đăng ký thất bại! Tên đăng nhập đã tồn tại.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        LoginView loginView = new LoginView(stage);
        loginView.show();
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}