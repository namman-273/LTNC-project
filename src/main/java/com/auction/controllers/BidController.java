package com.auction.controllers;

import com.auction.model.BidTransaction;
import com.auction.network.Protocol;
import com.auction.util.AlertUtil;
import com.auction.util.NotificationManager;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.AutoBidView;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class BidController implements Initializable {

    @FXML private Label auctionTitleLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private Label countdownLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private VBox snipingBox;
    @FXML private Label snipingCountdownLabel;
    @FXML private Label snipingCountLabel;

    private String auctionId;
    private String username;
    private long endTime;

    // Static cache giữ lịch sử khi quay lại
    private static final ConcurrentHashMap<String, ObservableList<String>> historyCache
            = new ConcurrentHashMap<>();

    private ObservableList<String> historyItems;
    private ServerConnection listenerConn;
    private Thread listenerThread;
    private Timeline snipingTimeline;
    private Timeline countdownTimeline;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username, long endTime) {
        this.auctionId = auctionId;
        this.username  = username;
        this.endTime   = endTime;

        auctionTitleLabel.setText("Phiên: " + auctionId);
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText(formatPrice(currentPrice));
        statusLabel.setText(status);

        // Lấy lịch sử từ cache hoặc tạo mới
        historyItems = historyCache.computeIfAbsent(
                auctionId, k -> FXCollections.observableArrayList());
        bidHistoryList.setItems(historyItems);

        // Gợi ý giá ban đầu
        try {
            double price = Double.parseDouble(
                    currentPrice.replace(",", "").replace(" VND", "").trim());
            long suggested = (long)(price + 1_000_000);
            bidAmountField.setPromptText("Gợi ý: " + String.format("%,d", suggested));
        } catch (NumberFormatException ignored) {}

        startCountdown();
        loadHistory();
        startListening();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    // ─── Countdown ───────────────────────────────────────────────────────────

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = endTime - System.currentTimeMillis();
            if (countdownLabel == null) return;

            if (remaining <= 0) {
                countdownLabel.setText("⏰ Phiên đã kết thúc!");
                countdownLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                countdownTimeline.stop();
                return;
            }

            long hours   = remaining / 3_600_000;
            long minutes = (remaining % 3_600_000) / 60_000;
            long seconds = (remaining % 60_000) / 1_000;
            countdownLabel.setText(
                    String.format("⏱ Còn: %02d:%02d:%02d", hours, minutes, seconds));

            if (remaining < 300_000) {
                countdownLabel.setStyle(
                        "-fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                countdownLabel.setStyle(
                        "-fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    // ─── Listener ────────────────────────────────────────────────────────────

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
                    Platform.runLater(() ->
                            AlertUtil.showError("Mất kết nối",
                                    "Không thể kết nối listener realtime!\nGiá sẽ không cập nhật tự động."));
                    return;
                }

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
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String newPrice = parts[2];
                    String bidder   = parts[3];
                    Platform.runLater(() -> {
                        currentPriceLabel.setText(formatPrice(newPrice));
                        historyItems.add(0, bidder + " đặt: " + formatPrice(newPrice));
                        // Cập nhật gợi ý giá
                        try {
                            double price = Double.parseDouble(newPrice);
                            long suggested = (long)(price + 1_000_000);
                            bidAmountField.setPromptText(
                                    "Gợi ý: " + String.format("%,d", suggested));
                        } catch (NumberFormatException ignored) {}
                    });
                }
                break;

            case Protocol.SNIPING:
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String count = parts[3];
                    try {
                        this.endTime = Long.parseLong(parts[2]);
                    } catch (NumberFormatException ignored) {}
                    Platform.runLater(() -> startSnipingCountdown(120, count));
                }
                break;

            case Protocol.RES_END_SUCCESS:
            case "END_AUCTION_SUCCESS":
                // END_SUCCESS|auctionId|Winner:xxx|Bid:yyy$
                Platform.runLater(() -> {
                    statusLabel.setText("FINISHED");
                    stopSnipingCountdown();
                    if (countdownTimeline != null) countdownTimeline.stop();
                    if (countdownLabel != null) {
                        countdownLabel.setText("⏰ Phiên đã kết thúc!");
                        countdownLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    }

                    // Parse winner
                    String detail = parts.length >= 3 ? parts[2] : "";
                    if (detail.contains("Winner:" + username)) {
                        // Mày thắng
                        String bid = detail.contains("Bid:")
                                ? detail.substring(detail.indexOf("Bid:") + 4) : "";
                        showSuccess("🎉 Bạn đã thắng phiên đấu giá!");
                        showNotification("🎉 Chúc mừng bạn đã thắng!",
                                "Bạn đã thắng phiên: " + auctionId
                                        + "\nGiá thắng: " + bid
                                        + "\nTiền đã bị trừ khỏi tài khoản.");
                    } else if (detail.contains("No winner")) {
                        showInfo("Phiên kết thúc — không có người thắng.");
                    } else {
                        showInfo("Phiên đã kết thúc. Bạn không thắng lần này.");
                    }
                });
                stopListener();
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

    // ─── History ─────────────────────────────────────────────────────────────

    private void loadHistory() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId
            );
            System.out.println("History: " + response);

            if (response == null || response.startsWith("ERROR|Mất kết nối")) {
                Platform.runLater(() ->
                        AlertUtil.showError("Mất kết nối", "Không thể tải lịch sử đặt giá!"));
                return;
            }

            if (response.startsWith(Protocol.RES_HISTORY)) {
                String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
                if (parts.length >= 3) {
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

    // ─── Actions ─────────────────────────────────────────────────────────────

    @FXML
    private void handleBid() {
        String amountStr = bidAmountField.getText().trim();

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_BID + Protocol.SEPARATOR + auctionId + Protocol.SEPARATOR + amountStr
            );
            System.out.println("BID response: " + response);

            Platform.runLater(() -> {
                if (response == null || response.startsWith("ERROR|Mất kết nối")) {
                    showError("Mất kết nối server!");
                    AlertUtil.showError("Mất kết nối",
                            "Mất kết nối khi đặt giá!\nVui lòng thử lại.");
                    return;
                }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BID_SUCCESS)
                        || response.startsWith(Protocol.UPDATE)) {
                    showSuccess("Đặt giá thành công!");


                    bidAmountField.clear();
                } else {
                    String errorMsg = parts.length > 1 ? parts[1] : "Đặt giá thất bại!";
                    showError(errorMsg);
                }

            });
        }).start();
    }

    @FXML
    private void handleAutoBid() {
        stopListener();
        stopSnipingCountdown();
        if (countdownTimeline != null) countdownTimeline.stop();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        new AutoBidView(stage, auctionId, itemNameLabel.getText(),
                currentPriceLabel.getText(), statusLabel.getText(), username, endTime).show();
    }

    @FXML
    private void handleBack() {
        stopListener();
        stopSnipingCountdown();
        if (countdownTimeline != null) countdownTimeline.stop();
        Platform.runLater(() -> {
            Stage stage = (Stage) bidAmountField.getScene().getWindow();
            new AuctionListView(stage, username).show();
        });
    }

    @FXML
    private void handleViewChart() {
        stopListener();
        stopSnipingCountdown();
        if (countdownTimeline != null) countdownTimeline.stop();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        new BidChartView(stage, auctionId, itemNameLabel.getText(),
                currentPriceLabel.getText(), statusLabel.getText(), username, endTime).show();
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

    private void showNotification(String title, String message) {
        NotificationManager.getInstance().add(title + ": " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    private void showInfo(String msg) {
        messageLabel.setStyle("-fx-text-fill: #1565C0; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}