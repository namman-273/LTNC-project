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
    private String password;

    public void setData(String auctionId, String itemName, String currentPrice, String status, String username, String password) {
        this.password = password;
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

        // Load lịch sử đặt giá
        loadHistory();
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
                ServerConnection listenerConn = new ServerConnection("localhost", 9999);
                listenerConn.connectDirect();
                listenerConn.sendAndReceive("LOGIN|" + username + "|" + password);

                while (!Thread.currentThread().isInterrupted()) {
                    String message = listenerConn.receive();
                    if (message == null) break;
                    System.out.println("Realtime: " + message);

                    if (message.startsWith("UPDATE|")) {
                        String[] parts = message.split("\\|");
                        if (parts.length >= 4 && parts[1].equals(auctionId)) {
                            String newPrice = parts[2];
                            String bidder = parts[3];
                            Platform.runLater(() -> {
                                currentPriceLabel.setText("Giá hiện tại: " + String.format("%,.0f VND", Double.parseDouble(newPrice)));
                                historyItems.add(0, bidder + " đặt: " + String.format("%,.0f VND", Double.parseDouble(newPrice)));
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
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) bidAmountField.getScene().getWindow();
            AuctionListView listView = new AuctionListView(stage, username);
            listView.show();
        });
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
    private void loadHistory() {
        new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection("localhost", 9999);
                conn.connectDirect();
                conn.sendAndReceive("LOGIN|" + username + "|dummy");
                String response = conn.sendAndReceive("GET_HISTORY|" + auctionId);
                System.out.println("History: " + response);
                System.out.println("History detail: [" + response + "]");

                if (response != null && response.startsWith("HISTORY_RES")) {
                    String[] parts = response.split("\\|", 3);
                    if (parts.length >= 3) {
                        String json = parts[2];
                        String[] entries = json.replace("[", "").replace("]", "").split("\\},\\{");
                        javafx.application.Platform.runLater(() -> {
                            for (String entry : entries) {
                                if (entry.contains("amount")) {
                                    String amount = entry.replaceAll(".*\"amount\":(\\d+\\.?\\d*).*", "$1");
                                    String bidder = entry.replaceAll(".*\"username\":\"([^\"]+)\".*", "$1");
                                    try {
                                        historyItems.add(bidder + " đặt: " + String.format("%,.0f VND", Double.parseDouble(amount)));
                                    } catch (Exception e) {
                                        historyItems.add(entry);
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi load history: " + e.getMessage());
            }
        }).start();
    }
}