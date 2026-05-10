package com.auction.controllers;

import com.auction.network.Protocol;
import com.auction.util.AlertUtil;
import com.auction.util.NotificationManager;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.AutoBidView;
import com.auction.views.BidChartView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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

public class BidController implements Initializable {

    @FXML private Label auctionTitleLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label itemTypeLabel;
    @FXML private Label sellerLabel;
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
    private double currentPriceValue = 0;

    private Consumer<String> pushListener;

    private static final ConcurrentHashMap<String, ObservableList<String>> historyCache
            = new ConcurrentHashMap<>();

    private ObservableList<String> historyItems;
    private Timeline snipingTimeline;
    private Timeline countdownTimeline;

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username, long endTime) {
        this.auctionId = auctionId;
        this.username  = username;
        this.endTime   = endTime;

        auctionTitleLabel.setText(itemName);
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText(formatPrice(currentPrice));
        statusLabel.setText(status);

        try {
            currentPriceValue = Double.parseDouble(
                    currentPrice.replace(",", "").replace(" VND", "").trim());
        } catch (NumberFormatException ignored) {}

        updateBidSuggestion(currentPriceValue);

        historyItems = historyCache.computeIfAbsent(
                auctionId, k -> FXCollections.observableArrayList());
        bidHistoryList.setItems(historyItems);

        startCountdown();
        loadHistory();
        registerPushListener();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    // ─── Push Listener ───────────────────────────────────────────────────────

    private void registerPushListener() {
        pushListener = this::handleServerPush;
        ServerConnection.getInstance().addPushListener(pushListener);
    }

    private void handleServerPush(String message) {
        String[] parts = message.split("\\" + Protocol.SEPARATOR);
        if (parts.length == 0) return;

        switch (parts[0]) {

            case Protocol.NOTI_BID_UPDATE:
                // BID_UPDATE|auctionId|newPrice|bidderUsername
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String newPrice = parts[2];
                    String bidder   = parts[3];
                    boolean isMe    = bidder.equals(username);
                    Platform.runLater(() -> {
                        try { currentPriceValue = Double.parseDouble(newPrice); }
                        catch (Exception ignored) {}
                        currentPriceLabel.setText(formatPrice(newPrice));
                        // FIX: push BID_UPDATE tự thêm vào history — không cần loadHistory()
                        historyItems.add(0, (isMe ? "⭐ Bạn" : "👤 " + bidder)
                                + "  •  " + formatPrice(newPrice));
                        updateBidSuggestion(currentPriceValue);
                    });
                }
                break;

            case Protocol.NOTI_SNIPING_UPDATE:
                // SNIPING_UPDATE|auctionId|newEndTime|extensionCount
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String count = parts[3];
                    try { this.endTime = Long.parseLong(parts[2]); }
                    catch (NumberFormatException ignored) {}
                    Platform.runLater(() -> startSnipingCountdown(120, count));
                }
                break;

            case Protocol.NOTI_OUTBID:
                // OUTBID|auctionId|newBidder|newAmount
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String newBidder = parts[2];
                    String newAmt    = parts[3];
                    Platform.runLater(() ->
                            showError("⚠️ Bạn bị vượt giá bởi " + newBidder
                                    + "! Giá mới: " + formatPrice(newAmt)));
                }
                break;

            case Protocol.NOTI_REFUND:
                // REFUND|auctionId|refundAmount|newBalance
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String refundAmt = parts[2];
                    String newBal    = parts[3];
                    Platform.runLater(() ->
                            showInfo("💰 Hoàn " + formatPrice(refundAmt)
                                    + " → Số dư: " + formatPrice(newBal)));
                }
                break;

            case Protocol.RES_END_SUCCESS:
                Platform.runLater(() -> {
                    statusLabel.setText("FINISHED");
                    stopSnipingCountdown();
                    if (countdownTimeline != null) countdownTimeline.stop();
                    if (countdownLabel != null) {
                        countdownLabel.setText("⏰ Hết giờ!");
                        countdownLabel.setStyle(
                                "-fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 28px;");
                    }
                    String detail = parts.length >= 3 ? parts[2] : "";
                    if (detail.contains("Winner:" + username)) {
                        showSuccess("🎉 Bạn đã thắng phiên đấu giá!");
                        showNotification("🎉 Chúc mừng!", "Bạn đã thắng phiên: " + auctionId);
                    } else if (detail.contains("No winner")) {
                        showInfo("Phiên kết thúc — không có người thắng.");
                    } else {
                        showInfo("Phiên đã kết thúc. Bạn không thắng lần này.");
                    }
                });
                removePushListener();
                break;

            default:
                break;
        }
    }

    private void removePushListener() {
        if (pushListener != null) {
            ServerConnection.getInstance().removePushListener(pushListener);
            pushListener = null;
        }
    }

    // ─── Quick-add buttons ────────────────────────────────────────────────────

    @FXML private void handleAdd1M()  { addAmount(1_000_000); }
    @FXML private void handleAdd5M()  { addAmount(5_000_000); }
    @FXML private void handleAdd10M() { addAmount(10_000_000); }

    private void addAmount(double amount) {
        try {
            String current = bidAmountField.getText().trim();
            double base = current.isEmpty() ? currentPriceValue
                    : Double.parseDouble(current.replace(",", "").replace(" VND", "").trim());
            bidAmountField.setText(String.valueOf((long)(base + amount)));
        } catch (NumberFormatException ignored) {
            bidAmountField.setText(String.valueOf((long)(currentPriceValue + amount)));
        }
    }

    private void updateBidSuggestion(double price) {
        long suggested = (long)(price + 1_000_000);
        bidAmountField.setPromptText("Gợi ý: " + String.format("%,d", suggested));
    }

    // ─── Countdown ────────────────────────────────────────────────────────────

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = endTime - System.currentTimeMillis();
            if (countdownLabel == null) return;
            if (remaining <= 0) {
                countdownLabel.setText("⏰ Hết giờ!");
                countdownLabel.setStyle(
                        "-fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 28px;");
                countdownTimeline.stop();
                return;
            }
            long hours   = remaining / 3_600_000;
            long minutes = (remaining % 3_600_000) / 60_000;
            long seconds = (remaining % 60_000) / 1_000;
            countdownLabel.setText(String.format("%02d : %02d : %02d", hours, minutes, seconds));
            countdownLabel.setStyle(remaining < 300_000
                    ? "-fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 28px; -fx-font-family: monospace;"
                    : "-fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-font-size: 28px; -fx-font-family: monospace;");
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    // ─── Sniping countdown ────────────────────────────────────────────────────

    private void startSnipingCountdown(int totalSeconds, String extensionCount) {
        stopSnipingCountdown();
        snipingBox.setVisible(true);
        snipingBox.setManaged(true);
        snipingCountLabel.setText("Lần gia hạn thứ: " + extensionCount);
        final int[] secondsLeft = {totalSeconds};
        snipingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft[0]--;
            snipingCountdownLabel.setText(String.format("Còn: %02d:%02d",
                    secondsLeft[0] / 60, secondsLeft[0] % 60));
            if (secondsLeft[0] <= 0) stopSnipingCountdown();
        }));
        snipingTimeline.setCycleCount(totalSeconds);
        snipingTimeline.play();
    }

    private void stopSnipingCountdown() {
        if (snipingTimeline != null) { snipingTimeline.stop(); snipingTimeline = null; }
        if (snipingBox != null) { snipingBox.setVisible(false); snipingBox.setManaged(false); }
    }

    // ─── History ──────────────────────────────────────────────────────────────

    /**
     * FIX: Dùng JsonParser thủ công thay vì gson.fromJson(BidTransaction[].class).
     * Lý do: BidTransaction chứa User object phức tạp — Gson deserialize
     * có thể không đọc được username nếu server serialize User khác cấu trúc FE expect.
     * Parse thủ công chỉ lấy đúng field cần thiết: bidder.username + amount.
     */
    private void loadHistory() {
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId);
            if (response == null || response.startsWith("ERROR")) return;
            if (!response.startsWith(Protocol.RES_HISTORY)) return;

            String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
            if (parts.length < 3) return;

            String json = parts[2].trim();
            if (json.isEmpty() || json.equals("[]")) return;

            try {
                JsonArray array = JsonParser.parseString(json).getAsJsonArray();
                Platform.runLater(() -> {
                    historyItems.clear();
                    // Đảo ngược — mới nhất lên đầu
                    for (int i = array.size() - 1; i >= 0; i--) {
                        JsonObject obj = array.get(i).getAsJsonObject();

                        // FIX: lấy username từ nested object bidder.username
                        String bidder = "---";
                        if (obj.has("bidder") && obj.get("bidder").isJsonObject()) {
                            JsonObject bidderObj = obj.get("bidder").getAsJsonObject();
                            if (bidderObj.has("username")) {
                                bidder = bidderObj.get("username").getAsString();
                            }
                        }

                        double amount = obj.has("amount")
                                ? obj.get("amount").getAsDouble() : 0;

                        boolean isMe = bidder.equals(username);
                        historyItems.add((isMe ? "⭐ Bạn" : "👤 " + bidder)
                                + "  •  " + String.format("%,.0f VND", amount));
                    }
                });
            } catch (Exception e) {
                System.err.println("Lỗi parse history: " + e.getMessage());
            }
        }).start();
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    @FXML
    private void handleBid() {
        String amountStr = bidAmountField.getText().trim();
        if (amountStr.isEmpty()) { showError("Vui lòng nhập giá!"); return; }

        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_BID + Protocol.SEPARATOR + auctionId
                            + Protocol.SEPARATOR + amountStr);
            Platform.runLater(() -> {
                if (response == null || response.startsWith("ERROR|Mất kết nối")) {
                    showError("Mất kết nối server!"); return;
                }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BID_SUCCESS)) {
                    showSuccess("✅ Đặt giá thành công!");
                    bidAmountField.clear();
                    loadHistory(); // Reload vì server không gửi BID_UPDATE cho chính người vừa bid
                } else {
                    showError(parts.length > 1 ? parts[1] : "Đặt giá thất bại!");
                }
            });
        }).start();
    }

    @FXML
    private void handleAutoBid() {
        stopAll();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        new AutoBidView(stage, auctionId, itemNameLabel.getText(),
                currentPriceLabel.getText(), statusLabel.getText(), username, endTime).show();
    }

    @FXML
    private void handleBack() {
        stopAll();
        Platform.runLater(() -> {
            Stage stage = (Stage) bidAmountField.getScene().getWindow();
            new AuctionListView(stage, username).show();
        });
    }

    @FXML
    private void handleViewChart() {
        stopAll();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        new BidChartView(stage, auctionId, itemNameLabel.getText(),
                currentPriceLabel.getText(), statusLabel.getText(), username, endTime).show();
    }

    private void stopAll() {
        removePushListener();
        stopSnipingCountdown();
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String formatPrice(String raw) {
        try {
            return String.format("%,.0f VND",
                    Double.parseDouble(raw.replace(",", "").replace(" VND", "").trim()));
        } catch (NumberFormatException e) { return raw; }
    }

    private void showError(String msg) {
        if (messageLabel == null) return;
        messageLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        if (messageLabel == null) return;
        messageLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showInfo(String msg) {
        if (messageLabel == null) return;
        messageLabel.setStyle("-fx-text-fill: #1565C0; -fx-font-size: 12px;");
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
}
