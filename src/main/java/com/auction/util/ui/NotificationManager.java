package com.auction.util.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton quản lý thông báo toàn app.
 * Hỗ trợ đọc/chưa đọc, phân loại tab, lưu auctionId để mở BidView.
 */
public class NotificationManager {

    // ── Inner model ──────────────────────────────────────────────────────────
    public static class NotificationItem {
        private final String message;
        private final String time;
        private final String category;   // "auction" | "system" | "balance"
        private final String auctionId;  // null nếu không liên quan đến phiên cụ thể
        private boolean read;

        public NotificationItem(String message, String category, String auctionId) {
            this.message   = message;
            this.category  = (category != null) ? category : detectCategory(message);
            this.auctionId = auctionId;
            this.read      = false;
            this.time      = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        }

        // Compat: không có auctionId
        public NotificationItem(String message, String category) {
            this(message, category, null);
        }

        private static String detectCategory(String msg) {
            if (msg == null) return "system";
            if (msg.contains("thắng") || msg.contains("vượt giá") || msg.contains("OUTBID")
                    || msg.contains("gia hạn") || msg.contains("Hoàn tiền")
                    || msg.contains("đặt giá") || msg.contains("BID")
                    || msg.contains("Hoàn")) {
                return "auction";
            } else if (msg.contains("nạp") || msg.contains("số dư")
                    || msg.contains("BALANCE") || msg.contains("REFUND")
                    || msg.contains("VNĐ")) {
                return "balance";
            }
            return "system";
        }

        public String getMessage()   { return message;   }
        public String getTime()      { return time;      }
        public String getCategory()  { return category;  }
        public String getAuctionId() { return auctionId; }
        public boolean isRead()      { return read;      }
        public void markRead()       { this.read = true; }

        @Override
        public String toString() {
            return time + "  •  " + message;
        }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static NotificationManager instance;

    private final List<NotificationItem>           items           = new ArrayList<>();
    private final ObservableList<NotificationItem> observableItems =
            FXCollections.observableArrayList();
    // Legacy compat cho các màn hình cũ dùng ObservableList<String>
    private final ObservableList<String>            legacyList      =
            FXCollections.observableArrayList();

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Thêm thông báo, tự detect category, không có auctionId. */
    public void add(String message) {
        add(message, null, null);
    }

    /** Thêm thông báo với category tường minh, không có auctionId. */
    public void add(String message, String category) {
        add(message, category, null);
    }

    /** Thêm thông báo đầy đủ với auctionId để mở BidView. */
    public void add(String message, String category, String auctionId) {
        NotificationItem item = new NotificationItem(message, category, auctionId);
        items.add(0, item);
        observableItems.add(0, item);
        legacyList.add(0, item.toString());
    }

    public ObservableList<NotificationItem> getObservableItems() { return observableItems; }
    public ObservableList<String>           getAll()             { return legacyList; }
    public int  size()        { return items.size(); }
    public long unreadCount() { return items.stream().filter(i -> !i.isRead()).count(); }

    public void markAllRead() {
        items.forEach(NotificationItem::markRead);
        observableItems.setAll(new ArrayList<>(items));
    }

    public void remove(NotificationItem item) {
        items.remove(item);
        observableItems.remove(item);
        legacyList.remove(item.toString());
    }

    public void clearRead() {
        List<NotificationItem> dead = new ArrayList<>();
        for (NotificationItem i : items) if (i.isRead()) dead.add(i);
        items.removeAll(dead);
        observableItems.removeAll(dead);
        dead.forEach(i -> legacyList.remove(i.toString()));
    }

    public void clear() {
        items.clear();
        observableItems.clear();
        legacyList.clear();
    }
}