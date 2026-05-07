package com.auction.dto;

public class AuctionRow {
    private final String id;
    private final String itemName;
    private final double currentPrice;
    private final String status;
    private final long endTime;
    private final String sellerId;

    public AuctionRow(String id, String itemName, double currentPrice,
                      String status, long endTime) {
        this(id, itemName, currentPrice, status, endTime, "");
    }

    public AuctionRow(String id, String itemName, double currentPrice,
                      String status, long endTime, String sellerId) {
        this.id           = id;
        this.itemName     = itemName;
        this.currentPrice = currentPrice;
        this.status       = status;
        this.endTime      = endTime;
        this.sellerId     = sellerId != null ? sellerId : "";
    }

    public String getId()           { return id; }
    public String getItemName()     { return itemName; }
    public double getCurrentPrice() { return currentPrice; }
    public String getStatus()       { return status; }
    public long getEndTime()        { return endTime; }
    public String getSellerId()     { return sellerId; }

    public String getCurrentPriceFormatted() {
        return String.format("%,.0f VNĐ", currentPrice);
    }
}