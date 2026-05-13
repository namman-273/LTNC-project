package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.protocol.Protocol;
import com.auction.network.client.ServerConnection;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BidView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class WatchlistController implements Initializable {

    @FXML private TableView<AuctionRow> watchlistTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Label messageLabel;
    @FXML private VBox watchlistCards;
    @FXML private Label totalCountLabel;
    @FXML private Label openCountLabel;
    @FXML private Label finishedCountLabel;

    private String username;
    private ObservableList<AuctionRow> currentData = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setUsername(String username) {
        this.username = username;
        loadWatchlist();
        startAutoRefresh();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Giữ columns để controller không crash khi table ẩn
        if (idCol != null)     idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (nameCol != null)   nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        if (priceCol != null)  priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPriceFormatted"));
        if (statusCol != null) statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadWatchlist() {
        showMessage("Đang tải watchlist...", "#6B7280");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(Protocol.CMD_GET_WATCHLIST);
            System.out.println("Watchlist response: " + response);

            Platform.runLater(() -> {
                if (response == null) {
                    showMessage("❌ Mất kết nối server!", "#DC2626");
                    return;
                }

                ObservableList<AuctionRow> data = FXCollections.observableArrayList();

                if (response.startsWith(Protocol.RES_WATCHLIST)) {
                    String json = response.substring(
                            Protocol.RES_WATCHLIST.length() + Protocol.SEPARATOR.length());
                    AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
                    if (rows != null) data.addAll(rows);
                } else {
                    String[] parts = response.split("\\" + Protocol.SEPARATOR);
                    String msg = parts.length > 1 ? parts[1] : "Lỗi tải watchlist!";
                    showMessage("❌ " + msg, "#DC2626");
                    return;
                }

                currentData.setAll(data);
                if (watchlistTable != null) watchlistTable.setItems(data);
                updateCards(data);

                if (data.isEmpty()) {
                    showMessage("Chưa có phiên nào trong watchlist.", "#6B7280");
                } else {
                    showMessage("✅ " + data.size() + " phiên đang theo dõi.", "#059669");
                }
            });
        }).start();
    }

    private void updateCards(ObservableList<AuctionRow> data) {
        if (watchlistCards == null) return;
        watchlistCards.getChildren().clear();

        long open     = data.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
        long finished = data.stream().filter(r -> "FINISHED".equals(r.getStatus())).count();

        if (totalCountLabel    != null) totalCountLabel.setText(String.valueOf(data.size()));
        if (openCountLabel     != null) openCountLabel.setText(String.valueOf(open));
        if (finishedCountLabel != null) finishedCountLabel.setText(String.valueOf(finished));

        if (data.isEmpty()) {
            VBox empty = new VBox();
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 60 0;");
            Label emptyLabel = new Label("⭐ Chưa có phiên nào trong watchlist");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF;");
            empty.getChildren().add(emptyLabel);
            watchlistCards.getChildren().add(empty);
            return;
        }

        for (AuctionRow row : data) {
            watchlistCards.getChildren().add(buildCard(row));
        }
    }

    private HBox buildCard(AuctionRow row) {
        boolean isOpen = "OPEN".equals(row.getStatus()) || "RUNNING".equals(row.getStatus());

        // Avatar
        String initials = row.getItemName().length() >= 2
                ? row.getItemName().substring(0, 2).toUpperCase()
                : row.getItemName().toUpperCase();
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: " + (isOpen ? "#DBEAFE" : "#F3F4F6") + ";"
                + "-fx-text-fill: " + (isOpen ? "#1565C0" : "#6B7280") + ";"
                + "-fx-font-size: 13px; -fx-font-weight: bold;"
                + "-fx-background-radius: 8; -fx-padding: 8 10;"
                + "-fx-min-width: 44; -fx-min-height: 44; -fx-alignment: CENTER;");

        // Info
        Label name = new Label(row.getItemName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Label price = new Label(row.getCurrentPriceFormatted());
        price.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        VBox info = new VBox(3, name, price);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Status badge
        Label status = new Label(row.getStatus());
        String badgeBg, badgeFg;
        switch (row.getStatus()) {
            case "OPEN":     badgeBg = "#D1FAE5"; badgeFg = "#059669"; break;
            case "RUNNING":  badgeBg = "#FEF3C7"; badgeFg = "#D97706"; break;
            case "FINISHED": badgeBg = "#F3F4F6"; badgeFg = "#6B7280"; break;
            default:         badgeBg = "#F3F4F6"; badgeFg = "#6B7280";
        }
        status.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + ";"
                + "-fx-background-radius: 20; -fx-padding: 3 10;");

        // Nút Chọn (để select vào table ẩn — phục vụ handleViewDetail/handleUnwatch)
        Button btnSelect = new Button("Chọn");
        btnSelect.setStyle("-fx-background-color: transparent; -fx-text-fill: #1565C0;"
                + "-fx-font-size: 11px; -fx-cursor: hand;"
                + "-fx-border-color: #DBEAFE; -fx-border-radius: 6; -fx-border-width: 1;"
                + "-fx-background-radius: 6; -fx-padding: 4 10;");
        btnSelect.setOnAction(e -> {
            if (watchlistTable != null) watchlistTable.getSelectionModel().select(row);
            highlightSelected(row);
        });

        HBox card = new HBox(12, avatar, info, status, btnSelect);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;"
                + "-fx-cursor: hand;");
        VBox.setMargin(card, new Insets(0, 0, 8, 0));

        card.setOnMouseClicked(e -> {
            if (watchlistTable != null) watchlistTable.getSelectionModel().select(row);
            highlightSelected(row);
        });

        return card;
    }

    private void highlightSelected(AuctionRow selected) {
        if (watchlistCards == null) return;
        watchlistCards.getChildren().forEach(node -> {
            if (node instanceof HBox) {
                HBox card = (HBox) node;
                // Reset style
                card.setStyle(card.getStyle().replace(
                        "-fx-border-color: #1565C0;", "-fx-border-color: #E5E7EB;"));
            }
        });
        // Không thể match card với row dễ dàng — dùng index
        int idx = currentData.indexOf(selected);
        if (idx >= 0 && idx < watchlistCards.getChildren().size()) {
            HBox card = (HBox) watchlistCards.getChildren().get(idx);
            card.setStyle(card.getStyle().replace(
                    "-fx-border-color: #E5E7EB;", "-fx-border-color: #1565C0;"));
        }
    }

    @FXML
    public void handleRefresh() { loadWatchlist(); }

    @FXML
    public void handleUnwatch() {
        AuctionRow selected = watchlistTable != null
                ? watchlistTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showMessage("⚠️ Vui lòng chọn một phiên trước!", "#D97706");
            return;
        }
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selected.getId());
            Platform.runLater(() -> {
                if (response == null) { showMessage("❌ Mất kết nối!", "#DC2626"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
                    showMessage("✅ " + (parts.length > 1 ? parts[1] : "Đã bỏ theo dõi!"), "#059669");
                    loadWatchlist();
                } else {
                    showMessage("❌ " + (parts.length > 1 ? parts[1] : "Thất bại!"), "#DC2626");
                }
            });
        }).start();
    }

    @FXML
    public void handleViewDetail() {
        AuctionRow selected = watchlistTable != null
                ? watchlistTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showMessage("⚠️ Vui lòng chọn một phiên trước!", "#D97706");
            return;
        }
        Stage stage = (Stage) (watchlistCards != null
                ? watchlistCards.getScene().getWindow()
                : watchlistTable.getScene().getWindow());
        new BidView(stage, selected.getId(), selected.getItemName(),
                String.valueOf(selected.getCurrentPrice()), selected.getStatus(),
                username, selected.getEndTime()).show();
    }

    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> loadWatchlist()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
    }

    @FXML
    public void handleBack() {
        stopAutoRefresh();
        Stage stage = (Stage) (watchlistCards != null
                ? watchlistCards.getScene().getWindow()
                : watchlistTable.getScene().getWindow());
        new AuctionListView(stage, username).show();
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }
}