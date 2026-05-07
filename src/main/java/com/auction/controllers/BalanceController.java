package com.auction.controllers;

import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class BalanceController implements Initializable {

    @FXML private Label balanceLabel;
    @FXML private Label usernameLabel;
    @FXML private Label messageLabel;
    @FXML private TextField depositAmountField;

    private String username;

    public void setUsername(String username) {
        this.username = username;
        usernameLabel.setText("Tài khoản: " + username);
        loadBalance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    private void loadBalance() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            // Dùng Protocol.CMD_GET_BALANCE
            String response = conn.sendAndReceive(Protocol.CMD_GET_BALANCE);
            System.out.println("Balance response: " + response);

            Platform.runLater(() -> {
                if (response == null) {
                    showMessage("Mất kết nối server!", "red");
                    return;
                }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BALANCE_INFO) && parts.length > 1) {
                    // Lấy balance từ BE trả về
                    try {
                        double balance = Double.parseDouble(parts[1]);
                        balanceLabel.setText(String.format("%,.0f VNĐ", balance));
                        balanceLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
                    } catch (NumberFormatException e) {
                        balanceLabel.setText(parts[1]);
                    }
                } else {
                    String msg = parts.length > 1 ? parts[1] : "Lỗi tải số dư!";
                    showMessage("❌ " + msg, "red");
                }
            });
        }).start();
    }

    @FXML
    private void handleDeposit() {
        // Không tự validate — gửi thẳng lên BE
        String amount = depositAmountField.getText().trim();

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            // Dùng Protocol.CMD_DEPOSIT
            String response = conn.sendAndReceive(
                    Protocol.CMD_DEPOSIT + Protocol.SEPARATOR
                            + username + Protocol.SEPARATOR
                            + amount
            );
            System.out.println("Deposit response: " + response);

            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_DEPOSIT_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Nạp tiền thành công!";
                    showMessage("✅ " + msg, "green");
                    depositAmountField.clear();
                    loadBalance();
                } else {
                    String msg = parts.length > 1 ? parts[1] : "Nạp tiền thất bại!";
                    showMessage("❌ " + msg, "red");
                }
            });
        }).start();
    }

    @FXML
    private void handleRefresh() { loadBalance(); }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) balanceLabel.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }
}
