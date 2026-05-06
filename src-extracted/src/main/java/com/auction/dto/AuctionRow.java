package com.auction.dto;

/**
 * DTO dùng chung cho FE để hiển thị danh sách phiên đấu giá.
 * Trước đây AuctionListController và AdminDashboardController mỗi nơi
 * định nghĩa một inner class AuctionRow riêng — giờ dùng chung class này.
 */
public class AuctionRow {
    private final String id;
    private final String itemName;
    private final String currentPrice;
    private final String status;

    public AuctionRow(String id, String itemName, String currentPrice, String status) {
        this.id           = id;
        this.itemName     = itemName;
        this.currentPrice = currentPrice;
        this.status       = status;
    }

    public String getId()           { return id; }
    public String getItemName()     { return itemName; }
    public String getCurrentPrice() { return currentPrice; }
    public String getStatus()       { return status; }
}