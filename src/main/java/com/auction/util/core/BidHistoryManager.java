package com.auction.util.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton lưu lịch sử các phiên đấu giá mà user đã tham gia (lưu local in-memory).
 * Dữ liệu được ghi vào đây từ BidController khi nhận RES_END_SUCCESS.
 */
public class BidHistoryManager {

    public enum Result { WIN, LOSE }

    // ── Inner DTO ─────────────────────────────────────────────────────────────
    public static class HistoryRecord {
        private final String auctionId;
        private final String itemName;
        private final String itemType;
        private final String finalPrice;
        private final String date;
        private final Result result;

        public HistoryRecord(String auctionId, String itemName, String itemType,
                             String finalPrice, Result result) {
            this.auctionId  = auctionId;
            this.itemName   = itemName;
            this.itemType   = itemType != null ? itemType : "—";
            this.finalPrice = finalPrice;
            this.result     = result;
            this.date       = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        public String getAuctionId()  { return auctionId;  }
        public String getItemName()   { return itemName;   }
        public String getItemType()   { return itemType;   }
        public String getFinalPrice() { return finalPrice; }
        public String getDate()       { return date;       }
        public Result getResult()     { return result;     }
        public boolean isWin()        { return result == Result.WIN; }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static BidHistoryManager instance;
    private final List<HistoryRecord> records = new ArrayList<>();

    private BidHistoryManager() {}

    public static BidHistoryManager getInstance() {
        if (instance == null) instance = new BidHistoryManager();
        return instance;
    }

    // ── API ───────────────────────────────────────────────────────────────────

    public void addRecord(String auctionId, String itemName, String itemType,
                          String finalPrice, Result result) {
        // Tránh trùng lặp: nếu auctionId đã có thì bỏ qua
        boolean exists = records.stream().anyMatch(r -> r.getAuctionId().equals(auctionId));
        if (!exists) {
            records.add(0, new HistoryRecord(auctionId, itemName, itemType, finalPrice, result));
        }
    }

    public List<HistoryRecord> getAll() {
        return new ArrayList<>(records);
    }

    public List<HistoryRecord> getWins() {
        return records.stream().filter(HistoryRecord::isWin).collect(Collectors.toList());
    }

    public List<HistoryRecord> getLoses() {
        return records.stream().filter(r -> !r.isWin()).collect(Collectors.toList());
    }

    public int totalCount() { return records.size(); }
    public int winCount()   { return (int) records.stream().filter(HistoryRecord::isWin).count(); }
    public int loseCount()  { return totalCount() - winCount(); }
    public String winRate() {
        if (totalCount() == 0) return "0%";
        return String.format("%.0f%%", (winCount() * 100.0 / totalCount()));
    }
}