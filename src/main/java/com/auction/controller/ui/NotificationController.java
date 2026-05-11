package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.util.ui.NotificationManager.NotificationItem;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BidView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML private ListView<NotificationItem> notificationList;
    @FXML private Label  countLabel;
    @FXML private Label  unreadCountLabel;
    @FXML private Button tabAll;
    @FXML private Button tabUnread;
    @FXML private Button tabAuction;
    @FXML private Button tabSystem;

    private String username;
    private String activeTab = "all";

    private static final String TAB_ACTIVE =
            "-fx-background-color: #1565C0; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 20; " +
                    "-fx-padding: 6 18; -fx-cursor: hand; -fx-font-size: 12px;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #6B7280; " +
                    "-fx-background-radius: 20; -fx-padding: 6 18; " +
                    "-fx-cursor: hand; -fx-font-size: 12px;";

    public void setUsername(String u) { this.username = u; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationList.setCellFactory(lv -> new NotificationCell());
        applyTab("all");
        updateCount();

        NotificationManager.getInstance().getObservableItems()
                .addListener((javafx.collections.ListChangeListener<NotificationItem>) c -> {
                    Platform.runLater(() -> {
                        updateCount();
                        applyTab(activeTab);
                    });
                });
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────
    @FXML private void handleTabAll()     { applyTab("all");     }
    @FXML private void handleTabUnread()  { applyTab("unread");  }
    @FXML private void handleTabAuction() { applyTab("auction"); }
    @FXML private void handleTabSystem()  { applyTab("system");  }

    private void applyTab(String tab) {
        this.activeTab = tab;
        tabAll.setStyle(TAB_INACTIVE);
        tabUnread.setStyle(TAB_INACTIVE);
        tabAuction.setStyle(TAB_INACTIVE);
        tabSystem.setStyle(TAB_INACTIVE);

        ObservableList<NotificationItem> src = NotificationManager.getInstance().getObservableItems();
        ObservableList<NotificationItem> filtered = FXCollections.observableArrayList();

        switch (tab) {
            case "unread":
                tabUnread.setStyle(TAB_ACTIVE);
                src.stream().filter(i -> !i.isRead()).forEach(filtered::add);
                break;
            case "auction":
                tabAuction.setStyle(TAB_ACTIVE);
                src.stream().filter(i -> "auction".equals(i.getCategory())).forEach(filtered::add);
                break;
            case "system":
                tabSystem.setStyle(TAB_ACTIVE);
                src.stream()
                        .filter(i -> "system".equals(i.getCategory()) || "balance".equals(i.getCategory()))
                        .forEach(filtered::add);
                break;
            default:
                tabAll.setStyle(TAB_ACTIVE);
                filtered.addAll(src);
                break;
        }
        notificationList.setItems(filtered);
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML private void handleMarkAll() {
        NotificationManager.getInstance().markAllRead();
        updateCount();
        applyTab(activeTab);
    }

    @FXML private void handleClearRead() {
        NotificationManager.getInstance().clearRead();
        updateCount();
        applyTab(activeTab);
    }

    @FXML private void handleBack() {
        Stage stage = (Stage) notificationList.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private void updateCount() {
        long unread = NotificationManager.getInstance().unreadCount();
        int  total  = NotificationManager.getInstance().size();

        if (countLabel != null)
            countLabel.setText(total + " thông báo");

        if (unreadCountLabel != null) {
            if (unread > 0) {
                unreadCountLabel.setText(unread + " chưa đọc");
                unreadCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FED7AA;");
            } else {
                unreadCountLabel.setText("Tất cả đã đọc");
                unreadCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.6);");
            }
        }
    }

    /**
     * Mở BidView cho một auctionId.
     * Gọi LIST_AUCTIONS để lấy thông tin mới nhất rồi mở.
     */
    private void openBidView(String auctionId) {
        if (auctionId == null || auctionId.isEmpty()) return;
        Stage stage = (Stage) notificationList.getScene().getWindow();

        new Thread(() -> {
            try {
                String resp = ServerConnection.getInstance()
                        .sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
                if (resp == null || !resp.startsWith(Protocol.RES_LIST_SUCCESS)) return;

                String json = resp.substring(resp.indexOf(Protocol.SEPARATOR) + 1);
                Gson gson = new Gson();
                Type listType = new TypeToken<List<AuctionRow>>(){}.getType();
                List<AuctionRow> rows = gson.fromJson(json, listType);

                AuctionRow target = rows.stream()
                        .filter(r -> auctionId.equals(r.getId()))
                        .findFirst().orElse(null);

                Platform.runLater(() -> {
                    if (target != null) {
                        new BidView(stage,
                                target.getId(),
                                target.getItemName(),
                                String.valueOf(target.getCurrentPrice()),
                                target.getStatus(),
                                username,
                                target.getEndTime()).show();
                    } else {
                        // Phiên không còn tồn tại → về danh sách
                        new AuctionListView(stage, username).show();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> new AuctionListView(stage, username).show());
            }
        }, "open-bid-thread").start();
    }

    // ── Custom Cell ───────────────────────────────────────────────────────────
    private class NotificationCell extends ListCell<NotificationItem> {

        private final HBox  root      = new HBox(12);
        private final Label iconLabel = new Label();
        private final VBox  content   = new VBox(4);
        private final HBox  titleRow  = new HBox(8);
        private final Label message   = new Label();
        private final Label newBadge  = new Label("Mới");
        private final Label timeLabel = new Label();
        private final HBox  actionRow = new HBox(8);
        private final Button bidBtn   = new Button("Đặt giá ngay");
        private final Button delBtn   = new Button("✕");

        NotificationCell() {
            // Icon circle
            iconLabel.setPrefSize(40, 40);
            iconLabel.setMinSize(40, 40);
            iconLabel.setMaxSize(40, 40);
            iconLabel.setAlignment(Pos.CENTER);

            // Badge "Mới"
            newBadge.setStyle(
                    "-fx-background-color: #EA580C; -fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 10; -fx-padding: 1 7;");

            message.setWrapText(true);
            message.setMaxWidth(300);

            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

            // Nút "Đặt giá ngay"
            bidBtn.setStyle(
                    "-fx-background-color: #EA6C0A; -fx-text-fill: white;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 14; -fx-padding: 4 14; -fx-cursor: hand;");
            bidBtn.setOnAction(e -> {
                NotificationItem item = getItem();
                if (item == null) return;
                item.markRead();
                openBidView(item.getAuctionId());
            });

            // Nút xóa riêng
            delBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #C0C4CC;" +
                            "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4;");
            delBtn.setOnAction(e -> {
                NotificationItem item = getItem();
                if (item == null) return;
                NotificationManager.getInstance().remove(item);
                updateCount();
                applyTab(activeTab);
            });

            titleRow.setAlignment(Pos.CENTER_LEFT);
            titleRow.getChildren().addAll(message, newBadge);
            HBox.setHgrow(message, Priority.ALWAYS);

            actionRow.setAlignment(Pos.CENTER_LEFT);

            HBox.setHgrow(content, Priority.ALWAYS);
            content.getChildren().addAll(titleRow, timeLabel, actionRow);

            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 14, 12, 14));
            root.getChildren().addAll(iconLabel, content, delBtn);

            // ── Click vào card → mark read + refresh tab ──────────────────
            setOnMouseClicked(e -> {
                NotificationItem item = getItem();
                if (item == null) return;
                if (!item.isRead()) {
                    item.markRead();
                    updateCount();
                    applyTab(activeTab); // nếu đang ở tab "Chưa đọc" thì nó biến mất
                }
            });

            setGraphic(root);
            setText(null);
        }

        @Override
        protected void updateItem(NotificationItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setStyle(""); return; }

            message.setText(item.getMessage());
            timeLabel.setText(item.getTime());

            // Badge + font weight theo trạng thái đọc
            boolean unread = !item.isRead();
            newBadge.setVisible(unread);
            newBadge.setManaged(unread);
            message.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;" +
                    (unread ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;"));

            // Xác định style theo loại notification
            String iconTxt, iconBg, iconColor, cardBg, borderColor;
            boolean showBid;
            String msg = item.getMessage();

            if (msg.contains("vượt giá") || msg.contains("OUTBID")) {
                iconTxt = "!"; iconBg = "#FEE2E2"; iconColor = "#EF4444";
                cardBg = unread ? "#FFF1F1" : "#FFF9F9";
                borderColor = "#EF4444"; showBid = true;
            } else if (msg.contains("thắng") || msg.contains("Winner")) {
                iconTxt = "★"; iconBg = "#D1FAE5"; iconColor = "#16A34A";
                cardBg = unread ? "#F0FDF4" : "#F9FFFC";
                borderColor = "#22C55E"; showBid = false;
            } else if (msg.contains("gia hạn") || msg.contains("sắp kết thúc")) {
                iconTxt = "t"; iconBg = "#FEF9C3"; iconColor = "#CA8A04";
                cardBg = unread ? "#FEFCE8" : "#FEFDF5";
                borderColor = "#EAB308"; showBid = true;
            } else if (msg.contains("Hoàn") || msg.contains("REFUND") || msg.contains("nạp")
                    || msg.contains("số dư") || msg.contains("VNĐ")) {
                iconTxt = "$"; iconBg = "#DBEAFE"; iconColor = "#2563EB";
                cardBg = unread ? "#EFF6FF" : "#F5F8FF";
                borderColor = "#3B82F6"; showBid = false;
            } else if ("auction".equals(item.getCategory())) {
                iconTxt = "A"; iconBg = "#EDE9FE"; iconColor = "#7C3AED";
                cardBg = unread ? "#F5F3FF" : "#F8F7FF";
                borderColor = "#8B5CF6"; showBid = item.getAuctionId() != null;
            } else {
                iconTxt = "i"; iconBg = "#F3F4F6"; iconColor = "#6B7280";
                cardBg = unread ? "white" : "#FAFAFA";
                borderColor = "#D1D5DB"; showBid = false;
            }

            iconLabel.setText(iconTxt);
            iconLabel.setStyle(
                    "-fx-background-radius: 20; -fx-font-size: 14px; -fx-font-weight: bold;" +
                            "-fx-text-fill: " + iconColor + ";" +
                            "-fx-background-color: " + iconBg + ";");

            // Border trái màu
            setStyle(
                    "-fx-background-color: " + cardBg + ";" +
                            "-fx-border-color: transparent transparent transparent " + borderColor + ";" +
                            "-fx-border-width: 0 0 0 4;" +
                            "-fx-background-insets: 0;");

            // Nút đặt giá: chỉ hiện nếu có auctionId và loại phù hợp
            actionRow.getChildren().clear();
            if (showBid && item.getAuctionId() != null) {
                actionRow.getChildren().add(bidBtn);
            }

            setGraphic(root);
            setText(null);
        }
    }
}