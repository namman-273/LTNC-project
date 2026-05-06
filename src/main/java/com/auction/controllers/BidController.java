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
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.BidChartView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    // FIX: Listener dùng connection riêng (cần nhận push từ server liên tục).
    // Nhưng KHÔNG login lại — chỉ cần subscribe vào auction sau khi connect.
    // Connection chính (getInstance) dùng để gửi BID, GET_HISTORY như bình thường.
    private ServerConnection listenerConn;
    private Thread listenerThread;

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username) {
        this.auctionId = auctionId;
        this.username  = username;

        auctionTitleLabel.setText("Phiên: " + auctionId);
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText(formatPrice(currentPrice));
        statusLabel.setText(status);
        bidHistoryList.setItems(historyItems);

        // Dùng connection chính (đã login sẵn) để load history
        loadHistory();

        // Listener dùng connection riêng để nhận UPDATE realtime
        startListening();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bidHistoryList.setItems(historyItems);
    }

    /**
     * FIX: Listener connect riêng nhưng KHÔNG login lại.
     * Server nhận kết nối mới → ClientHandler mới → ta chỉ cần gửi LOGIN
     * một lần duy nhất để server biết đây là ai và add observer.
     *
     * Lý do vẫn cần login trên listener connection:
     * Mỗi socket = một ClientHandler riêng trên server. ClientHandler mới
     * không có currentUser → phải login để server add observer cho đúng socket này.
     * Đây là login kỹ thuật (subscribe), không phải login lại từ đầu.
     * Password lấy từ SessionManager — không hardcode, không truyền qua tham số.
     */
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

                // Subscribe: login để server add observer cho socket này
                listenerConn.sendAndReceive("LOGIN|" + username + "|" + pwd);

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

    /**
     * Xử lý tất cả các loại push từ server trong một chỗ.
     * Thêm case mới (SNIPING, NEW_AUCTION...) chỉ cần thêm vào đây.
     */
    private void handleServerPush(String message) {
        String[] parts = message.split("\\|");
        if (parts.length == 0) return;

        switch (parts[0]) {
            case "UPDATE":
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

            case "SNIPING":
                // FIX: Xử lý anti-sniping — server gia hạn phiên, thông báo cho user
                // SNIPING|auctionId|newEndTime|extensionCount
                if (parts.length >= 4 && parts[1].equals(auctionId)) {
                    String count = parts[3];
                    Platform.runLater(() -> showInfo(
                            "⏱ Phiên được gia hạn thêm 2 phút! (lần " + count + ")"
                    ));
                }
                break;

            case "END_AUCTION_SUCCESS":
                // END_AUCTION_SUCCESS|auctionId|Winner:...|Bid: ...
                Platform.runLater(() -> {
                    statusLabel.setText("FINISHED");
                    showSuccess("Phiên đấu giá đã kết thúc! " +
                            (parts.length >= 3 ? parts[2] : ""));
                });
                stopListener();
                break;

            default:
                // Bỏ qua các message không liên quan (LOGIN_SUCCESS, LIST... từ server)
                break;
        }
    }

    /**
     * FIX: Dùng ServerConnection.getInstance() (đã login sẵn từ đầu).
     * Không tạo connection mới, không login lại.
     */
    private void loadHistory() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive("GET_HISTORY|" + auctionId);
            System.out.println("History: " + response);

            if (response != null && response.contains("HISTORY_RES")) {
                String[] parts = response.split("\\|", 3);
                if (parts.length >= 3) {
                    parseAndShowHistory(parts[2].trim());
                }
            }
        }).start();
    }

    private void parseAndShowHistory(String json) {
        if (json.equals("[]") || json.isEmpty()) return;
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            Platform.runLater(() -> {
                historyItems.clear();
                for (JsonElement el : array) {
                    JsonObject obj  = el.getAsJsonObject();
                    double amount   = obj.has("amount")   ? obj.get("amount").getAsDouble()          : 0;
                    String bidder   = obj.has("bidder") && obj.get("bidder").isJsonObject()
                            ? obj.get("bidder").getAsJsonObject().get("username").getAsString()
                            : "---";
                    historyItems.add(bidder + " đặt: " + formatPrice(String.valueOf(amount)));
                }
            });
        } catch (Exception e) {
            System.err.println("Parse history error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBid() {
        String amountStr = bidAmountField.getText().trim();
        if (amountStr.isEmpty()) { showError("Vui lòng nhập giá!"); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showError("Giá không hợp lệ!");
            return;
        }

        // FIX: Dùng connection chính — không tạo thêm connection
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
        stopListener();
        Platform.runLater(() -> {
            Stage stage = (Stage) bidAmountField.getScene().getWindow();
            new AuctionListView(stage, username).show();
        });
    }

    @FXML
    private void handleViewChart() {
        stopListener();
        Stage stage = (Stage) bidAmountField.getScene().getWindow();
        new BidChartView(stage, auctionId, itemNameLabel.getText(),
                currentPriceLabel.getText(), statusLabel.getText(), username).show();
    }

    private void stopListener() {
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        // listenerConn.disconnectDirect() xử lý trong finally của listenerThread
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

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