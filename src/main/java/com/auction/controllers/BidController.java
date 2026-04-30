package com.auction.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.views.AuctionListView;

import java.net.URL;
import java.util.ResourceBundle;

public class BidController implements Initializable {

    @FXML private Label auctionTitleLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;

    private String auctionId;
    private String username;
    private ObservableList<String> historyItems = FXCollections.observableArrayList();
    private Thread listenerThread;

    public void setData(String auctionId, String itemName, String currentPrice, String status, String username) {
        this.auctionId = auctionId;
        this.username = username;
        auctionTitleLabel.setText("Phiên: " + auctionId);
        itemNameLabel.setText("Tên: " + itemName);
        try {
            double price = Double.parseDouble(currentPrice.replace(",", "").replace(" VND", ""));
            currentPriceLabel.setText("Giá hiện tại: " + String.format("%,.0f VND", price));
        } catch (NumberFormatException e) {
            currentPriceLabel.setText("Giá hiện tại: " + currentPrice);
        }
        statusLabel.setText("Trạng thái: " + status);
        bidHistoryList.setItems(historyItems);

        // Bắt đầu lắng nghe update từ server
        startListening();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bidHistoryList.setItems(historyItems);
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();
                while (true) {
                    String message = conn.receive();
                    if (message == null) break;

                    System.out.println("Realtime: " + message);

                    if (message.startsWith("UPDATE|")) {
                        String[] parts = message.split("\\|");
                        if (parts.length >= 4 && parts[1].equals(auctionId)) {
                            String newPrice = parts[2];
                            String bidder = parts[3];
                            Platform.runLater(() -> {
                                currentPriceLabel.setText("Giá hiện tại: " + String.format("%,.0f VND", Double.parseDouble(newPrice)));
                                historyItems.add(0, bidder + " đặt: " + newPrice);
                            });
                        }
                    } else if (message.startsWith("END_AUCTION_SUCCESS|")) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Trạng thái: FINISHED");
                            messageLabel.setStyle("-fx-text-fill: green;");
                            messageLabel.setText("Phiên đấu giá đã kết thúc!");
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Listener error: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @FXML
    private void handleBid() {
        String amount = bidAmountField.getText().trim();
        if (amount.isEmpty()) {
            showError("Vui lòng nhập giá!");
            return;
        }

        try {
            Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            showError("Giá không hợp lệ!");
            return;
        }

        // Chạy trên thread riêng để không block UI
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive("BID|" + auctionId + "|" + amount);
            System.out.println("BID response: " + response);

            Platform.runLater(() -> {
                if (response != null && (response.startsWith("BID_SUCCESS") || response.startsWith("UPDATE"))) {
                    showSuccess("Đặt giá thành công!");
                    bidAmountField.clear();
                } else {
                    showError("Đặt giá thất bại! " + (response != null ? response : ""));
                }
            });
        }).start();
    }

    @FXML
    private void handleBack() {
        if (listenerThread != null) listenerThread.interrupt();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        AuctionListView listView = new AuctionListView(stage, username);
        listView.show();
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