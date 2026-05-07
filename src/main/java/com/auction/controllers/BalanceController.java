package com.auction.controllers;

import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.views.AuctionListView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BalanceController implements Initializable {

    @FXML private Label balanceLabel;
    @FXML private Label usernameLabel;
    @FXML private Label messageLabel;
    @FXML private TextField depositAmountField;
    @FXML private ListView<String> transactionList;

    private String username;
    private final ObservableList<String> transactions = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void setUsername(String username) {
        this.username = username;
        usernameLabel.setText("Tài khoản: " + username);
        loadBalance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (transactionList != null) {
            transactionList.setItems(transactions);
        }
    }

    private void loadBalance() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(Protocol.CMD_GET_BALANCE);

            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BALANCE_INFO) && parts.length > 1) {
                    try {
                        double balance = Double.parseDouble(parts[1]);
                        balanceLabel.setText(String.format("%,.0f VNĐ", balance));
                        balanceLabel.setStyle(
                                "-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
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
        String amount = depositAmountField.getText().trim();

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_DEPOSIT + Protocol.SEPARATOR + amount
            );

            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_DEPOSIT_SUCCESS)) {
                    String msg = parts.length > 2 ? parts[2] : "Nạp tiền thành công!";
                    showMessage("✅ " + msg, "green");
                    depositAmountField.clear();
                    // Thêm vào lịch sử giao dịch
                    try {
                        double amt = Double.parseDouble(amount);
                        String time = LocalDateTime.now().format(FORMATTER);
                        transactions.add(0, "+" + String.format("%,.0f VNĐ", amt)
                                + "  •  Nạp tiền  •  " + time + "  ✅ Thành công");
                    } catch (NumberFormatException ignored) {}
                    loadBalance();
                } else {
                    String msg = parts.length > 1 ? parts[1] : "Nạp tiền thất bại!";
                    showMessage("❌ " + msg, "red");
                    // Thêm giao dịch thất bại
                    try {
                        double amt = Double.parseDouble(amount);
                        String time = LocalDateTime.now().format(FORMATTER);
                        transactions.add(0, String.format("%,.0f VNĐ", amt)
                                + "  •  Nạp tiền  •  " + time + "  ❌ Thất bại");
                    } catch (NumberFormatException ignored) {}
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
