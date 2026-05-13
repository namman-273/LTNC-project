package com.auction.model.dto;

import com.auction.model.entities.Auction;

public class AuctionRow {
  private final String id;
  private final String itemName;
  private final double currentPrice;
  private final String status;
  private final long endTime;
  private final String sellerId;
  private final String description;
  private final String imageUrl;
  private final String itemType;
  private final double startingPrice;

  public AuctionRow(Auction a) {
    this.id = a.getId();
    this.endTime = a.getEndTime();
    this.itemName = a.getItem().getItemName() != null ? a.getItem().getItemName() : "---";
    this.currentPrice = a.getCurrentPrice();
    this.status = a.getStatus().toString();
    String rawSellerId = a.getSellerId();
    this.sellerId = (rawSellerId != null && !rawSellerId.trim().isEmpty())
        ? rawSellerId
        : "Anonymous";

    this.description = a.getItem().getDescription();
    this.imageUrl = a.getItem().getImageUrl();
    this.itemType = a.getItem().getClass().getSimpleName();
    this.startingPrice = a.getItem().getStartingPrice();

  }

  public String getItemType() {
    return itemType;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public String getStartingPriceFormatted() {
    return String.format("%,.0f VND", startingPrice);

  }

  public String getId() {
    return id;
  }

  public String getItemName() {
    return itemName;
  }

  public double getCurrentPrice() {
    return currentPrice;
  }

  public String getStatus() {
    return status;
  }

  public long getEndTime() {
    return endTime;
  }

  public String getSellerId() {
    return sellerId;
  }

  public String getDescription() {
    return description;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getCurrentPriceFormatted() {
    return String.format("%,.0f VNĐ", currentPrice);
  }
}