package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.protocol.Protocol;
import com.auction.network.client.ServerConnection;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.CreateAuctionView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AdminDashboardController implements Initializable {

    @FXML private TableView<AuctionRow>           auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Label     messageLabel;
    @FXML private Label     statTotalLabel;
    @FXML private Label     statOpenLabel;
    @FXML private Label     statFinishedLabel;

    private String   username;
    private Timeline autoRefreshTimeline;
    private Consumer<String> pushListener;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
                    case "OPEN"     -> setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    case "RUNNING"  -> setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
                    case "FINISHED" -> setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    case "CANCELED" -> setStyle("-fx-text-fill: #888888; -fx-font-weight: bold;");
                    default         -> setStyle("-fx-text-fill: #888888;");
                }
            }
        });

        loadFromServer();
        registerPushListener();
        startAutoRefresh();
    }

    // ── Push Listener ────────────────────────────────────────────────────────
    // RES_ADMIN_END_SUCCESS: response khi Admin bấm "Kết thúc phiên"
    //   (BE đã tách header riêng để isPushMessage route đúng, không timeout sendAndReceive)
    // NOTI_AUCTION_CANCELLED: broadcast khi admin cancel phiên còn thời gian
    private void registerPushListener() {
        pushListener = message -> {
            String[] parts = message.split("\\|");
            String header = parts[0];
            switch (header) {
                case Protocol.RES_ADMIN_END_SUCCESS -> {
                    String auctionId = parts.length >= 2 ? parts[1] : "";
                    Platform.runLater(() -> {
                        showMessage("✅ Phiên " + auctionId + " đã kết thúc thành công!", "green");
                        loadFromServer();
                    });
                }
                case Protocol.NOTI_AUCTION_CANCELLED -> {
                    String detail = parts.length >= 2 ? parts[1] : "Phiên đã bị hủy";
                    Platform.runLater(() -> {
                        showMessage("✅ " + detail, "green");
                        loadFromServer();
                    });
                }
                default -> {}
            }
        };
        ServerConnection.getInstance().addPushListener(pushListener);
    }

    private void removePushListener() {
        if (pushListener != null) {
            ServerConnection.getInstance().removePushListener(pushListener);
            pushListener = null;
        }
    }

    // ── Load danh sách phiên ─────────────────────────────────────────────────
    public void loadFromServer() {
        showMessage("Đang tải danh sách...", "gray");
        new Thread(() -> {
            String response = ServerConnection.getInstance()
                    .sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
            ObservableList<AuctionRow> data = FXCollections.observableArrayList();
            if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                String json = response.substring(
                        Protocol.RES_LIST_SUCCESS.length() + Protocol.SEPARATOR.length());
                AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
                if (rows != null) data.addAll(rows);
            }
            final ObservableList<AuctionRow> finalData = data;
            Platform.runLater(() -> {
                auctionTable.setItems(finalData);
                long openCount     = finalData.stream().filter(r -> "OPEN".equals(r.getStatus()) || "RUNNING".equals(r.getStatus())).count();
                long finishedCount = finalData.stream().filter(r -> "FINISHED".equals(r.getStatus()) || "PAID".equals(r.getStatus()) || "CANCELED".equals(r.getStatus())).count();
                if (statTotalLabel    != null) statTotalLabel.setText(String.valueOf(finalData.size()));
                if (statOpenLabel     != null) statOpenLabel.setText(String.valueOf(openCount));
                if (statFinishedLabel != null) statFinishedLabel.setText(String.valueOf(finishedCount));
                showMessage(finalData.isEmpty()
                        ? "ℹ️ Chưa có phiên nào."
                        : "✅ Tải xong " + finalData.size() + " phiên.", "gray");
            });
        }).start();
    }

    @FXML public void handleRefresh() { loadFromServer(); }

    // FIX MẤT KẾT NỐI: dùng sendAndReceive bình thường — giờ đã OK vì:
    // BE đổi response header thành RES_ADMIN_END_SUCCESS (không còn bị
    // isPushMessage nuốt vào pushListeners trước khi responseQueue kịp poll)
    @FXML
    public void handleEndAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên để kết thúc!", "red"); return;
        }
        showMessage("Đang kết thúc phiên " + selected.getId() + "...", "orange");
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_END_AUCTION + Protocol.SEPARATOR + selected.getId());
            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\|");
                if (response.startsWith(Protocol.RES_ADMIN_END_SUCCESS)) {
                    showMessage("✅ " + (parts.length > 1 ? parts[1] : "Kết thúc phiên thành công!"), "green");
                    loadFromServer();
                } else {
                    showMessage("❌ " + (parts.length > 1 ? parts[1] : "Lỗi kết thúc phiên!"), "red");
                }
            });
        }).start();
    }

    @FXML
    public void handleCreateAuction() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new CreateAuctionView(stage, username).show();
    }

    @FXML
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> loadFromServer()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) { autoRefreshTimeline.stop(); autoRefreshTimeline = null; }
    }

    public void handleBack() {
        stopAutoRefresh();
        removePushListener();
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }
}