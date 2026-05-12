package com.auction.model.dto;

import java.io.Serializable;

public class BidHistoryEntry implements Serializable {
  private static final long serialVersionUID = 1L;
  private String auctionId;
  private String itemName;
  private double finalPrice;
  private String result; // 
  private String endTime;

  public BidHistoryEntry(String auctionId, String itemName, double finalPrice, String result, String endTime) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.finalPrice = finalPrice;
    this.result = result;
    this.endTime = endTime;
  }
  // Getters để GSON serialize
}