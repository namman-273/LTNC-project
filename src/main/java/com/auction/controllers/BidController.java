package com.auction.controllers;

import com.auction.model.BidTransaction;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.BidChartView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class BidController implements Initializable {

    @FXML private Label auctionTitleLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidIncrementField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private VBox snipingBox;
    @FXML private Label snipingCountdownLabel;
    @FXML private Label snipingCountLabel;

    private String auctionId;
    private String username;
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    private ServerConnection listenerConn;
    private Thread listenerThread;
    private Timeline snipingTimeline;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username) {
        this.auctionId = auctionId;
        this.username  = username;

        auctionTitleLabel.setText("Phiên: " + auctionId);
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText(formatPrice(currentPrice));
        statusLabel.setText(status);
        bidHistoryList.setItems(historyItems);

        loadHistory();
        startListening();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bidHistoryList.setItems(historyItems);
    }

    private void startListening() {
        String pwd = SessionManager.getInstance().getPassword();
        if (pwd == null) {
            System.err.println("Listener: không có session, bỏ qua.");
            return;
        }

        listenerThread = new Thread(() -> {
            listenerConn = new ServerConnection("localhost", 9999);
            try {
                if (!listenerConn.connectDirect()) {
                    System.err.println("Listener: không thể kết nối.");
                    return;
                }

                // Dùng Protocol constants
                listenerConn.sendAndReceive(
                        Protocol.CMD_LOGIN + Protocol.SEPARATOR + username
                                + Protocol.SEPARATOR + pwd
                );

                while (!Thread.currentThread().isInterrupted()) {
                    String message = listenerConn.receive();
                    if (message == null) break;
                    System.out.println("Realtime: " + message);
                    handleServerPush(message);
                }
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Listener error: " + e.getMessage());
                }
            } finally {
                if (listenerConn != null) listenerConn.disconnectDirect();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerPush(String message) {
        String[] parts = message.split("\\" + Protocol.SEPARATOR);
        if (parts.length == 0) return;

        switch (parts[0]) {
            case Protocol.UPDATE:
                // UPDATE|auctionId|newPrice|bidderUsername
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String newPrice = parts[2];
                    String bidder   = parts[3];
                    Platform.runLater(() -> {
                        currentPriceLabel.setText(formatPrice(newPrice));
                        historyItems.add(0, bidder + " đặt: " + formatPrice(newPrice));
                    });
                }
                break;

            case Protocol.SNIPING:
                // SNIPING|auctionId|newEndTime|extensionCount
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String count = parts[3];
                    Platform.runLater(() -> startSnipingCountdown(120, count));
                }
                break;

            case Protocol.RES_END_SUCCESS:
                Platform.runLater(() -> {
                    statusLabel.setText("FINISHED");
                    showSuccess("Phiên đấu giá đã kết thúc! " +
                            (parts.length >= 3 ? parts[2] : ""));
                    stopSnipingCountdown();
                });
                stopListener();
                break;

            default:
                break;
        }
    }

    private void startSnipingCountdown(int totalSeconds, String extensionCount) {
        stopSnipingCountdown();
        snipingBox.setVisible(true);
        snipingBox.setManaged(true);
        snipingCountLabel.setText("Lần gia hạn thứ: " + extensionCount);

        final int[] secondsLeft = {totalSeconds};

        snipingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft[0]--;
            int min = secondsLeft[0] / 60;
            int sec = secondsLeft[0] % 60;
            snipingCountdownLabel.setText(String.format("Còn: %02d:%02d", min, sec));
            if (secondsLeft[0] <= 0) stopSnipingCountdown();
        }));
        snipingTimeline.setCycleCount(totalSeconds);
        snipingTimeline.play();
    }

    private void stopSnipingCountdown() {
        if (snipingTimeline != null) {
            snipingTimeline.stop();
            snipingTimeline = null;
        }
        if (snipingBox != null) {
            snipingBox.setVisible(false);
            snipingBox.setManaged(false);
        }
    }

    private void loadHistory() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            // Dùng Protocol.CMD_GET_HISTORY
            String response = conn.sendAndReceive(
                    Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId
            );
            System.out.println("History: " + response);

            if (response != null && response.startsWith(Protocol.RES_HISTORY)) {
                String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
                if (parts.length >= 3) {
                    // Dùng Gson + BidTransaction model của BE
                    BidTransaction[] history = gson.fromJson(parts[2].trim(), BidTransaction[].class);
                    if (history != null) {
                        Platform.runLater(() -> {
                            historyItems.clear();
                            for (BidTransaction bt : history) {
                                String bidder = bt.getBidder() != null
                                        ? bt.getBidder().getUsername() : "---";
                                historyItems.add(bidder + " đặt: "
                                        + String.format("%,.0f VND", bt.getAmount()));
                            }
                        });
                    }
                }
            }
        }).start();
    }

    @FXML
    private void handleBid() {
        // Không tự validate — gửi thẳng lên BE
        String amountStr = bidAmountField.getText().trim();

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_BID + Protocol.SEPARATOR + auctionId + Protocol.SEPARATOR + amountStr
            );
            System.out.println("BID response: " + response);

            Platform.runLater(() -> {
                if (response == null) { showError("Mất kết nối server!"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BID_SUCCESS)) {
                    showSuccess("Đặt giá thành công!");
                    bidAmountField.clear();
                } else {
                    String errorMsg = parts.length > 1 ? parts[1] : "Đặt giá thất bại!";
                    showError(errorMsg);
                }
            });
        }).start();
    }

    // ─── Auto-bid ────────────────────────────────────────────────────────────

    @FXML
    private void handleAutoBid() {
        // Không tự validate — gửi thẳng lên BE
        // BE expect: ADD_AUTO_BID|auctionId|maxBid|increment
        String maxBid     = autoBidMaxField != null ? autoBidMaxField.getText().trim() : "";
        String increment  = autoBidIncrementField != null ? autoBidIncrementField.getText().trim() : "";

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_ADD_AUTO_BID + Protocol.SEPARATOR
                            + auctionId + Protocol.SEPARATOR
                            + maxBid    + Protocol.SEPARATOR
                            + increment
            );
            System.out.println("AUTO_BID response: " + response);

            Platform.runLater(() -> {
                if (response == null) { showError("Mất kết nối server!"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_AUTO_BID_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Đặt auto-bid thành công!";
                    showSuccess(msg);
                    if (autoBidMaxField != null) autoBidMaxField.clear();
                    if (autoBidIncrementField != null) autoBidIncrementField.clear();
                } else {
                    String errorMsg = parts.length > 1 ? parts[1] : "Đặt auto-bid thất bại!";
                    showError(errorMsg);
                }
            });
        }).start();
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        stopListener();
        stopSnipingCountdown();
        Platform.runLater(() -> {
            Stage stage = (Stage) bidAmountField.getScene().getWindow();
            new AuctionListView(stage, username).show();
        });
    }

    @FXML
    private void handleViewChart() {
        stopListener();
        stopSnipingCountdown();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        new BidChartView(stage, auctionId, itemNameLabel.getText(),
                currentPriceLabel.getText(), statusLabel.getText(), username).show();
    }

    private void stopListener() {
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String formatPrice(String raw) {
        try {
            double price = Double.parseDouble(
                    raw.replace(",", "").replace(" VND", "").trim());
            return String.format("%,.0f VND", price);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showInfo(String msg) {
        messageLabel.setStyle("-fx-text-fill: #E65100; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}