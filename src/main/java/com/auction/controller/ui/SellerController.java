package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.network.client.ServerConnection;
import com.auction.util.core.SessionManager;
import com.auction.views.java.AuctionListView;
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

        auctionTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) loadHistory(newVal.getId(), newVal.getItemName());
                }
        );

        loadMyAuctions();
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
                if (parts.length >= 4) {
                    String auctionId = parts[1];
                    String newPrice  = parts[2];
                    String bidder    = parts[3];
                    boolean isMine = auctionData.stream()
                            .anyMatch(a -> a.getId().equals(auctionId));
                    if (isMine) {
                        Platform.runLater(() -> {
                            loadMyAuctions();
                            showNotification("🔔 Có bid mới!",
                                    bidder + " vừa đặt giá "
                                            + String.format("%,.0f VNĐ", Double.parseDouble(newPrice))
                                            + " tại phiên: " + auctionId);
                        });
                    }
                }
                break;

            case Protocol.RES_END_SUCCESS:
                if (parts.length >= 3) {
                    String auctionId = parts[1];
                    boolean isMine = auctionData.stream()
                            .anyMatch(a -> a.getId().equals(auctionId));
                    if (isMine) {
                        String detail = parts.length > 2 ? parts[2] : "";
                        Platform.runLater(() -> {
                            loadMyAuctions();
                            showNotification("🎉 Phiên đấu giá kết thúc!",
                                    "Phiên " + auctionId + " đã kết thúc!\n"
                                            + detail + "\nTiền đã được chuyển vào tài khoản.");
                        });
                    }
                }
                break;

            default:
                break;
        }
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
}