package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.network.client.ServerConnection;
import com.auction.util.core.SessionManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BidHistoryView;
import com.auction.views.java.BalanceView;
import com.auction.views.java.ProfileView;
import com.auction.views.java.NotificationView;
import com.auction.views.java.CreateAuctionView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class SellerController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label statsLabel;
    @FXML private Label statTotal;
    @FXML private Label statOpen;
    @FXML private Label statFinished;
    @FXML private Label statRevenue;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private ListView<String> historyList;
    @FXML private Label historyTitleLabel;
    @FXML private Label messageLabel;

    private final ObservableList<AuctionRow> auctionData = FXCollections.observableArrayList();
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private String username;
    private Consumer<String> pushListener;
    private Timeline autoRefreshTimeline;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        username = SessionManager.getInstance().getUsername();
        welcomeLabel.setText("Xin chào, " + username + "!");

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPriceFormatted"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "OPEN":     setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;"); break;
                    case "RUNNING":  setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;"); break;
                    case "FINISHED": setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;"); break;
                    default:         setStyle("-fx-text-fill: #888888;");
                }
            }
        });

        auctionTable.setItems(auctionData);
        historyList.setItems(historyData);
        historyList.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                if (item.equals("Đang tải...") || item.equals("Chưa có lịch sử đặt giá.") || item.equals("Lỗi tải lịch sử.")) {
                    setText(null);
                    javafx.scene.control.Label lbl = new javafx.scene.control.Label(item);
                    lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF; -fx-padding: 8 0;");
                    setGraphic(lbl);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // Parse "1. Duong  →  7,000,000 VNĐ"
                String[] parts = item.split("\\.", 2);
                String num = parts.length > 0 ? parts[0].trim() : "";
                String rest = parts.length > 1 ? parts[1].trim() : item;
                String[] arrowParts = rest.split("→", 2);
                String bidder = arrowParts.length > 0 ? arrowParts[0].trim() : rest;
                String price  = arrowParts.length > 1 ? arrowParts[1].trim() : "";

                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                        "-fx-border-color: #F3F4F6; -fx-border-radius: 10; -fx-border-width: 0.5;");

                // Số thứ tự
                javafx.scene.layout.StackPane numCircle = new javafx.scene.layout.StackPane();
                numCircle.setPrefSize(28, 28);
                numCircle.setMinSize(28, 28);
                numCircle.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 14;");
                javafx.scene.control.Label numLbl = new javafx.scene.control.Label(num);
                numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
                numCircle.getChildren().add(numLbl);

                // Tên bidder
                javafx.scene.control.Label bidderLbl = new javafx.scene.control.Label(bidder);
                bidderLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
                javafx.scene.layout.HBox.setHgrow(bidderLbl, javafx.scene.layout.Priority.ALWAYS);
                bidderLbl.setMaxWidth(Double.MAX_VALUE);

                // Giá
                javafx.scene.control.Label priceLbl = new javafx.scene.control.Label(price);
                priceLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");

                row.getChildren().addAll(numCircle, bidderLbl, priceLbl);

                javafx.scene.layout.VBox outer = new javafx.scene.layout.VBox(row);
                outer.setPadding(new javafx.geometry.Insets(0, 0, 6, 0));
                setGraphic(outer);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            }
        });

        auctionTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) loadHistory(newVal.getId(), newVal.getItemName());
                }
        );

        loadMyAuctions();
        loadBalance();
        registerPushListener();
    }

    private void registerPushListener() {
        pushListener = message -> {
            System.out.println("[Seller Push]: " + message);
            handleServerPush(message);
        };
        ServerConnection.getInstance().addPushListener(pushListener);
        startAutoRefresh();
    }

    private void handleServerPush(String message) {
        String[] parts = message.split("\\" + Protocol.SEPARATOR);
        if (parts.length == 0) return;

        switch (parts[0]) {
            case Protocol.NOTI_BID_UPDATE:
                // Format: BID_UPDATE|auctionId|newPrice|bidder
                // FIX: Bỏ check isMine qua auctionData — race condition (data load async).
                // Dùng loadMyAuctions() để kiểm tra sau khi data về rồi mới notify.
                if (parts.length >= 4) {
                    String auctionId = parts[1];
                    String newPrice  = parts[2];
                    String bidder    = parts[3];
                    Platform.runLater(() -> {
                        // Sau khi reload mới check auctionId có thuộc seller không
                        loadMyAuctionsThenNotify(auctionId,
                                "🔔 Có bid mới tại phiên: " + auctionId,
                                bidder + " vừa đặt giá "
                                        + formatPrice(newPrice)
                                        + " tại phiên: " + auctionId);
                    });
                }
                break;

            // RES_END_SUCCESS KHÔNG thêm vào đây vì BE gửi qua Observer pattern —
            // seller không phải observer nên message này không bao giờ tới FE seller.
            // Thay vào đó dùng BALANCE_CHANGED (BE gửi trực tiếp qua ConnectionManager)
            // làm tín hiệu phiên kết thúc có người thắng.

            case Protocol.NOTI_BALANCE_CHANGED:
                // Format: BALANCE_CHANGED|auctionId|newBalance|+amount
                // Đây là message DUY NHẤT BE gửi thẳng cho seller qua ConnectionManager.
                // Bug 2 fix: gộp 2 alert thành 1 để không lặp; sửa tiêu đề đúng với seller;
                // fix format số tiền nhận (delta có thể có dấu + ở đầu, cần parse đúng).
                if (parts.length >= 4) {
                    String auctionId = parts[1];
                    String newBalance = parts[2];
                    String delta = parts[3];
                    Platform.runLater(() -> {
                        loadMyAuctions();
                        // Cập nhật balance label ngay lập tức
                        if (balanceLabel != null) {
                            try {
                                balanceLabel.setText(String.format("%,.0f VNĐ", Double.parseDouble(newBalance)));
                            } catch (NumberFormatException ignored) {}
                        }
                        // Format số tiền nhận - loại bỏ dấu + nếu có
                        String deltaClean = delta.startsWith("+") ? delta.substring(1) : delta;
                        String deltaFormatted;
                        String balanceFormatted;
                        try {
                            deltaFormatted = String.format("%,.0f VNĐ", Double.parseDouble(deltaClean));
                        } catch (NumberFormatException e) {
                            deltaFormatted = deltaClean + " VNĐ";
                        }
                        try {
                            balanceFormatted = String.format("%,.0f VNĐ", Double.parseDouble(newBalance));
                        } catch (NumberFormatException e) {
                            balanceFormatted = newBalance + " VNĐ";
                        }
                        // Gộp 1 thông báo duy nhất, tiêu đề phù hợp với seller
                        showNotification("💰 Tiền đã về tài khoản!",
                                "Phiên " + auctionId + " đã kết thúc thành công.\n"
                                        + "Số tiền nhận: " + deltaFormatted + "\n"
                                        + "Số dư mới: " + balanceFormatted);
                    });
                }
                break;

            default:
                break;
        }
    }

    /** Reload data trước, sau đó chỉ notify nếu auctionId thực sự thuộc seller này. */
    private void loadMyAuctionsThenNotify(String auctionId, String title, String body) {
        new Thread(() -> {
            String response = ServerConnection.getInstance()
                    .sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
            if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                String json = response.substring(Protocol.RES_LIST_SUCCESS.length()
                        + Protocol.SEPARATOR.length());
                com.auction.model.dto.AuctionRow[] rows = gson.fromJson(json,
                        com.auction.model.dto.AuctionRow[].class);
                boolean isMine = rows != null && java.util.Arrays.stream(rows)
                        .anyMatch(r -> r.getId().equals(auctionId)
                                && username.equals(r.getSellerId()));
                Platform.runLater(() -> {
                    loadMyAuctions();
                    if (isMine) showNotification(title, body);
                });
            }
        }).start();
    }

    private String formatPrice(String raw) {
        try { return String.format("%,.0f VNĐ", Double.parseDouble(raw)); }
        catch (NumberFormatException e) { return raw + " VNĐ"; }
    }

    private void loadBalance() {
        new Thread(() -> {
            String res = ServerConnection.getInstance()
                    .sendAndReceive(Protocol.CMD_GET_BALANCE);
            Platform.runLater(() -> {
                if (balanceLabel == null) return;
                if (res != null && res.startsWith(Protocol.RES_BALANCE_INFO)) {
                    String[] p = res.split("\\|", -1);
                    String amt = p.length >= 2 ? p[1] : "---";
                    try {
                        balanceLabel.setText(String.format("%,.0f VNĐ", Double.parseDouble(amt)));
                    } catch (NumberFormatException e) {
                        balanceLabel.setText(amt + " VNĐ");
                    }
                } else {
                    balanceLabel.setText("---");
                }
            });
        }, "seller-balance-thread").start();
    }

    private void showNotification(String title, String message) {
        NotificationManager.getInstance().add(title + ": " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void loadMyAuctions() {
        new Thread(() -> {
            String response = ServerConnection.getInstance()
                    .sendAndReceive(Protocol.CMD_LIST_AUCTIONS);

            if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                String json = response.substring(Protocol.RES_LIST_SUCCESS.length()
                        + Protocol.SEPARATOR.length());
                AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);

                if (rows != null) {
                    ObservableList<AuctionRow> data = FXCollections.observableArrayList();
                    for (AuctionRow row : rows) {
                        if (username.equals(row.getSellerId())) {
                            data.add(row);
                        }
                    }
                    long open     = data.stream()
                            .filter(r -> "OPEN".equals(r.getStatus())).count();
                    long finished = data.stream()
                            .filter(r -> "FINISHED".equals(r.getStatus())
                                    || "PAID".equals(r.getStatus())).count();
                    double revenue = data.stream()
                            .filter(r -> "PAID".equals(r.getStatus()))
                            .mapToDouble(AuctionRow::getCurrentPrice)
                            .sum();

                    Platform.runLater(() -> {
                        auctionData.setAll(data);
                        if (statsLabel    != null) statsLabel.setText("(" + data.size() + " phiên)");
                        if (statTotal     != null) statTotal.setText(String.valueOf(data.size()));
                        if (statOpen      != null) statOpen.setText(String.valueOf(open));
                        if (statFinished  != null) statFinished.setText(String.valueOf(finished));
                        if (statRevenue   != null) statRevenue.setText(
                                revenue > 0 ? String.format("%,.0f VNĐ", revenue) : "---");
                    });
                }
            } else {
                Platform.runLater(() -> {
                    if (statsLabel != null) statsLabel.setText("Lỗi tải dữ liệu");
                });
            }
        }).start();
    }

    private void loadHistory(String auctionId, String itemName) {
        historyTitleLabel.setText("📋 Lịch sử - " + itemName);
        historyData.clear();
        historyData.add("Đang tải...");

        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId);

            Platform.runLater(() -> {
                historyData.clear();
                if (response == null || !response.startsWith(Protocol.RES_HISTORY)) {
                    historyData.add("Chưa có lịch sử đặt giá.");
                    return;
                }

                String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
                if (parts.length < 3 || parts[2].trim().equals("[]")) {
                    historyData.add("Chưa có lịch sử đặt giá.");
                    return;
                }

                try {
                    JsonArray array = JsonParser.parseString(parts[2].trim()).getAsJsonArray();
                    if (array.size() == 0) {
                        historyData.add("Chưa có lịch sử đặt giá.");
                        return;
                    }
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject obj = array.get(i).getAsJsonObject();
                        String bidder = "---";
                        if (obj.has("bidder") && obj.get("bidder").isJsonObject()) {
                            JsonObject bidderObj = obj.get("bidder").getAsJsonObject();
                            if (bidderObj.has("username")) {
                                bidder = bidderObj.get("username").getAsString();
                            }
                        }
                        double amount = obj.has("amount") ? obj.get("amount").getAsDouble() : 0;
                        historyData.add((i + 1) + ". " + bidder + "  →  "
                                + String.format("%,.0f VNĐ", amount));
                    }
                } catch (Exception e) {
                    historyData.add("Lỗi tải lịch sử.");
                }
            });
        }).start();
    }

    @FXML
    public void handleRefresh() {
        historyData.clear();
        historyTitleLabel.setText("📋 Lịch sử đặt giá");
        loadMyAuctions();
    }

    @FXML
    public void handleCreateAuction() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new CreateAuctionView(stage, username).show();
    }

    @FXML
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> loadMyAuctions())
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    @FXML
    public void handleBack() {
        if (pushListener != null) {
            ServerConnection.getInstance().removePushListener(pushListener);
            pushListener = null;
        }
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    public void setUsername(String username) {
        this.username = username;
        if (welcomeLabel != null)
            welcomeLabel.setText("Xin chào, " + username + "!");
    }

    @FXML
    public void handleBidHistory() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new BidHistoryView(stage, username).show();
    }

    @FXML
    public void handleBalance() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new BalanceView(stage, username).show();
    }

    @FXML
    public void handleProfile() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new ProfileView(stage, username).show();
    }

    @FXML
    public void handleNotification() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new NotificationView(stage, username).show();
    }

    @FXML
    public void handleLogout() {
        if (pushListener != null) {
            ServerConnection.getInstance().removePushListener(pushListener);
            pushListener = null;
        }
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        ServerConnection.getInstance().disconnect();
        SessionManager.getInstance().clear();
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new com.auction.views.java.LoginView(stage).show();
    }
}