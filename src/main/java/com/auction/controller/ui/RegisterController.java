package com.auction.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.network.protocol.Protocol;
import com.auction.network.client.ServerConnection;
import com.auction.views.java.LoginView;

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
        String username        = usernameField.getText().trim();
        String password        = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String role            = roleComboBox.getValue();

        // Check confirm password ở FE — BE không có field này
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

            // Dùng Protocol constants — đúng format BE expect: REGISTER|user|pass|role
            String response = conn.sendAndReceive(
                    Protocol.CMD_REGISTER + Protocol.SEPARATOR
                            + username + Protocol.SEPARATOR
                            + password + Protocol.SEPARATOR
                            + role
            );
            System.out.println("Server trả về: " + response);

            Platform.runLater(() -> {
                if (response == null) {
                    showError("Mất kết nối server!");
                    return;
                }

                String[] parts = response.split("\\" + Protocol.SEPARATOR);

                if (response.startsWith(Protocol.RES_REGISTER_SUCCESS)) {
                    // Lấy message từ BE: REGISTER_SUCCESS|Đăng ký thành công.
                    String msg = parts.length > 1 ? parts[1] : "Đăng ký thành công!";
                    showSuccess(msg + " Đang chuyển về đăng nhập...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> {
                                Stage stage = (Stage) usernameField.getScene().getWindow();
                                new LoginView(stage).show();
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                } else {
                    // Lấy message lỗi từ BE: REGISTER_FAILED|message hoặc ERROR|message
                    String errorMsg = parts.length > 1 ? parts[1] : "Đăng ký thất bại!";
                    showError(errorMsg);
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
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}