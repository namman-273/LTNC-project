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
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML private ListView<NotificationItem> notificationList;
    @FXML private Label  unreadCountLabel;
    @FXML private Button tabAll;
    @FXML private Button tabUnread;
    @FXML private Button tabAuction;
    @FXML private Button tabSystem;

    private String username;
    private String activeTab = "all";

    private static final String TAB_ACTIVE =
            "-fx-background-color: #111827; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 20; " +
                    "-fx-padding: 7 18; -fx-cursor: hand; -fx-font-size: 12px;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #6B7280; " +
                    "-fx-background-radius: 20; -fx-padding: 7 18; " +
                    "-fx-cursor: hand; -fx-font-size: 12px;";

    public void setUsername(String u) { this.username = u; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationList.setCellFactory(lv -> new NotificationCell());
        // Ẩn scrollbar ngang
        notificationList.setStyle(notificationList.getStyle() +
                "-fx-background-color: #F5F6FA; -fx-border-color: transparent;" +
                "-fx-background-insets: 0; -fx-padding: 12 14 12 14;");

        applyTab("all");
        updateCount();

        NotificationManager.getInstance().getObservableItems()
                .addListener((javafx.collections.ListChangeListener<NotificationItem>) c ->
                        Platform.runLater(() -> { updateCount(); applyTab(activeTab); }));
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

        ObservableList<NotificationItem> src =
                NotificationManager.getInstance().getObservableItems();
        ObservableList<NotificationItem> filtered = FXCollections.observableArrayList();

        switch (tab) {
            case "unread":
                tabUnread.setStyle(TAB_ACTIVE);
                src.stream().filter(i -> !i.isRead()).forEach(filtered::add);
                break;
            case "auction":
                tabAuction.setStyle(TAB_ACTIVE);
                src.stream().filter(i -> "auction".equals(i.getCategory()))
                        .forEach(filtered::add);
                break;
            case "system":
                tabSystem.setStyle(TAB_ACTIVE);
                src.stream()
                        .filter(i -> "system".equals(i.getCategory())
                                || "balance".equals(i.getCategory()))
                        .forEach(filtered::add);
                break;
            default:
                tabAll.setStyle(TAB_ACTIVE);
                filtered.addAll(src);
        }
        notificationList.setItems(filtered);
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML private void handleMarkAll() {
        NotificationManager.getInstance().markAllRead();
        updateCount(); applyTab(activeTab);
    }
    @FXML private void handleClearRead() {
        NotificationManager.getInstance().clearRead();
        updateCount(); applyTab(activeTab);
    }
    @FXML private void handleBack() {
        Stage stage = (Stage) notificationList.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private void updateCount() {
        long unread = NotificationManager.getInstance().unreadCount();
        int  total  = NotificationManager.getInstance().size();
        if (unreadCountLabel != null) {
            unreadCountLabel.setText(unread > 0
                    ? unread + " thông báo mới"
                    : total + " thông báo");
        }
    }

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
                        new BidView(stage, target.getId(), target.getItemName(),
                                String.valueOf(target.getCurrentPrice()),
                                target.getStatus(), username, target.getEndTime(),
                                target.getImageUrl() != null ? target.getImageUrl() : "",
                                target.getDescription() != null ? target.getDescription() : "",
                                target.getItemType() != null ? target.getItemType() : "",
                                target.getStartingPrice(),
                                target.getSellerId() != null ? target.getSellerId() : "").show();
                    } else {
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

        // ── Layout skeleton ──────────────────────────────────────────────────
        // Card wrapper (white bg, border-left, shadow, radius)
        private final HBox card       = new HBox(14);
        // Icon circle
        private final StackPane iconWrap = new StackPane();
        private final Label iconLabel = new Label();
        // Content
        private final VBox  content   = new VBox(3);
        // Row 1: title + badge
        private final HBox  titleRow  = new HBox(8);
        private final Label title     = new Label();
        private final Label newBadge  = new Label("Mới");
        // Row 2: subtitle/message detail
        private final Label subtitle  = new Label();
        // Row 3: time + actions
        private final HBox  bottomRow = new HBox(10);
        private final Label timeLabel = new Label();
        private final Button bidBtn   = new Button("Đặt giá ngay");
        // Delete button (outside card, top-right)
        private final Button delBtn   = new Button("⊘");

        NotificationCell() {
            // ── Icon circle ──
            iconWrap.setPrefSize(46, 46);
            iconWrap.setMinSize(46, 46);
            iconWrap.setMaxSize(46, 46);
            iconWrap.setAlignment(Pos.CENTER);
            iconLabel.setStyle("-fx-font-size: 20px;");
            iconWrap.getChildren().add(iconLabel);

            // ── Texts ──
            title.setWrapText(false);
            title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            newBadge.setStyle(
                    "-fx-background-color: #EA580C; -fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 12; -fx-padding: 2 8;");

            subtitle.setWrapText(true);
            subtitle.setMaxWidth(330);
            subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

            // ── Bid button ──
            bidBtn.setStyle(
                    "-fx-background-color: #EA6C0A; -fx-text-fill: white;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 16; -fx-padding: 5 16; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(234,108,10,0.3), 6, 0, 0, 2);");
            bidBtn.setOnAction(e -> {
                NotificationItem item = getItem();
                if (item == null) return;
                item.markRead();
                openBidView(item.getAuctionId());
            });

            // ── Delete button ──
            delBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #D1D5DB;" +
                            "-fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 2;");
            delBtn.setOnAction(e -> {
                NotificationItem item = getItem();
                if (item == null) return;
                NotificationManager.getInstance().remove(item);
                updateCount(); applyTab(activeTab);
            });

            // ── Assemble ──
            titleRow.setAlignment(Pos.CENTER_LEFT);
            titleRow.getChildren().addAll(title, newBadge);

            bottomRow.setAlignment(Pos.CENTER_LEFT);
            bottomRow.getChildren().add(timeLabel);

            content.getChildren().addAll(titleRow, subtitle, bottomRow);
            HBox.setHgrow(content, Priority.ALWAYS);

            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 14, 14, 16));
            card.getChildren().addAll(iconWrap, content, delBtn);
            card.setCursor(Cursor.HAND);

            // Outer wrapper (cell padding + card)
            VBox outer = new VBox(card);
            outer.setPadding(new Insets(0, 0, 8, 0));

            // ── Click card → mark read ──
            card.setOnMouseClicked(e -> {
                NotificationItem item = getItem();
                if (item == null) return;
                if (!item.isRead()) {
                    item.markRead();
                    updateCount();
                    applyTab(activeTab);
                }
            });

            setGraphic(outer);
            setText(null);
            // Transparent list cell bg
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        }

        @Override
        protected void updateItem(NotificationItem item, boolean empty) {
            super.updateItem(item, empty);
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            if (empty || item == null) { setGraphic(null); return; }

            boolean unread = !item.isRead();
            String  msg    = item.getMessage();

            // ── Classify ──────────────────────────────────────────────────
            String iconTxt, iconBg, borderColor;
            boolean showBid;
            String titleTxt, subtitleTxt;

            if (msg.contains("vượt giá") || msg.contains("OUTBID")) {
                iconTxt = "✕"; iconBg = "#FEE2E2"; borderColor = "#EF4444";
                titleTxt = "Bạn đã bị vượt giá";
                subtitleTxt = extractDetail(msg, "trong phiên");
                showBid = true;

            } else if (msg.contains("dẫn đầu") || msg.contains("cao nhất")) {
                iconTxt = "✓"; iconBg = "#DCFCE7"; borderColor = "#22C55E";
                titleTxt = "Bạn đang dẫn đầu";
                subtitleTxt = extractDetail(msg, null);
                showBid = false;

            } else if (msg.contains("gia hạn") || msg.contains("sắp kết thúc") || msg.contains("⏱")) {
                iconTxt = "⏰"; iconBg = "#FEF3C7"; borderColor = "#F59E0B";
                titleTxt = "Sắp kết thúc";
                subtitleTxt = extractDetail(msg, null);
                showBid = item.getAuctionId() != null;

                // FIX: tách rõ thông báo người THẮNG (có 🎉 hoặc "Chúc mừng")
                // khỏi thông báo phiên kết thúc chứa "Người chiến thắng: X" (người THUA đọc)
            } else if (msg.contains("🎉") || msg.contains("Chúc mừng")) {
                iconTxt = "★"; iconBg = "#FEF3C7"; borderColor = "#F59E0B";
                titleTxt = "Chúc mừng! Bạn đã thắng";
                subtitleTxt = extractDetail(msg, "phiên");
                showBid = false;

            } else if (msg.contains("không thắng") || msg.contains("Người chiến thắng")
                    || msg.contains("đã kết thúc") || msg.contains("kết thúc.")) {
                // Phiên kết thúc — người thua hoặc người theo dõi
                iconTxt = "◉"; iconBg = "#F3F4F6"; borderColor = "#E5E7EB";
                titleTxt = "Phiên kết thúc";
                subtitleTxt = extractDetail(msg, null);
                showBid = false;

            } else if (msg.contains("🔨") || msg.contains("giá mới")) {
                iconTxt = "🔨"; iconBg = "#EDE9FE"; borderColor = "#8B5CF6";
                titleTxt = "Giá mới trong phiên";
                subtitleTxt = extractDetail(msg, null);
                showBid = item.getAuctionId() != null;

            } else if (msg.contains("Hoàn") || msg.contains("REFUND")
                    || msg.contains("nạp") || msg.contains("VNĐ")) {
                iconTxt = "$"; iconBg = "#DBEAFE"; borderColor = "#3B82F6";
                titleTxt = "Cập nhật số dư ví";
                subtitleTxt = msg;
                showBid = false;

            } else {
                iconTxt = "◉"; iconBg = "#F3F4F6"; borderColor = "#E5E7EB";
                titleTxt = "Thông báo hệ thống";
                subtitleTxt = msg;
                showBid = false;
            }

            // ── Apply values ──────────────────────────────────────────────
            iconLabel.setText(iconTxt);
            iconLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;" +
                    (iconTxt.length() == 1 && !iconTxt.matches("[★◉⏰]")
                            ? "-fx-text-fill: " + borderColor + ";"
                            : "-fx-text-fill: " + borderColor + ";"));
            iconWrap.setStyle(
                    "-fx-background-color: " + iconBg + ";" +
                            "-fx-background-radius: 23;");

            title.setText(titleTxt);
            title.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;" +
                    (unread ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;"));

            subtitle.setText(subtitleTxt);

            newBadge.setVisible(unread);
            newBadge.setManaged(unread);

            timeLabel.setText(item.getTime() + " trước");

            // ── Card style: white bg + left border + shadow ───────────────
            String shadow = unread
                    ? "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 10, 0, 0, 3);"
                    : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);";
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: transparent transparent transparent " + borderColor + ";" +
                            "-fx-border-width: 0 0 0 4;" +
                            "-fx-border-radius: 0 14 14 0;" +
                            "-fx-background-insets: 0;" +
                            shadow);

            // ── Action row ────────────────────────────────────────────────
            bottomRow.getChildren().clear();
            bottomRow.getChildren().add(timeLabel);
            if (showBid && item.getAuctionId() != null) {
                Region spacer = new Region();
                spacer.setPrefWidth(10);
                bottomRow.getChildren().addAll(spacer, bidBtn);
            }

            // Restore graphic (needed after recycle)
            VBox outer = new VBox(card);
            outer.setPadding(new Insets(0, 0, 8, 0));
            setGraphic(outer);
            setText(null);
        }

        /** Rút gọn message dài thành subtitle. */
        private String extractDetail(String msg, String afterKeyword) {
            if (afterKeyword != null && msg.contains(afterKeyword)) {
                int idx = msg.indexOf(afterKeyword);
                return msg.substring(idx).trim();
            }
            // Bỏ emoji prefix
            String clean = msg.replaceAll("^[\\W]+", "").trim();
            return clean.length() > 70 ? clean.substring(0, 67) + "..." : clean;
        }
    }
}