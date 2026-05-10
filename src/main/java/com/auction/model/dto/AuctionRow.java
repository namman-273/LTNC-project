package com.auction.model.dto;

public class AuctionRow {
    private final String id;
    private final String itemName;
    private final double currentPrice;
    private final String status;
    private final long endTime;
    private final String sellerId;
    private final String imageUrl;      // Thêm mới
    private final String description;   // Thêm mới

    public AuctionRow(String id, String itemName, double currentPrice,
                      String status, long endTime) {
        this(id, itemName, currentPrice, status, endTime, "", "", "");
    }

    public AuctionRow(String id, String itemName, double currentPrice,
                      String status, long endTime, String sellerId) {
        this(id, itemName, currentPrice, status, endTime, sellerId, "", "");
    }

    public AuctionRow(String id, String itemName, double currentPrice,
                      String status, long endTime, String sellerId,
                      String imageUrl, String description) {
        this.id          = id;
        this.itemName    = itemName;
        this.currentPrice = currentPrice;
        this.status      = status;
        this.endTime     = endTime;
        this.sellerId    = sellerId != null ? sellerId : "";
        this.imageUrl    = imageUrl != null ? imageUrl : "";
        this.description = description != null ? description : "";
    }

    public String getId()           { return id; }
    public String getItemName()     { return itemName; }
    public double getCurrentPrice() { return currentPrice; }
    public String getStatus()       { return status; }
    public long getEndTime()        { return endTime; }
    public String getSellerId()     { return sellerId; }
    public String getImageUrl()     { return imageUrl; }
    public String getDescription()  { return description; }

    public String getCurrentPriceFormatted() {
        return String.format("%,.0f VNĐ", currentPrice);
    }
}