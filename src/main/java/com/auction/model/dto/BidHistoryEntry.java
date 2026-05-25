package com.auction.model.dto;

import java.io.Serializable;

/**
 *  * gửi đi dữ liệu đấu giá.
 *  
 */
public class BidHistoryEntry implements Serializable {
  private static final long serialVersionUID = 1L;
  private String auctionId;
  private String itemName;
  private double finalPrice;
  private String result; //
  private String endTime;

  /**
   *  * constructor.
   *  
   */
  public BidHistoryEntry(String auctionId, String itemName,
      double finalPrice, String result, String endTime) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.finalPrice = finalPrice;
    this.result = result;
    this.endTime = endTime;
  }

  // Getters để GSON serialize
  public String getAuctionId() {
    return auctionId;
  }

  public String getItemName() {
    return itemName;
  }

  public double getFinalPrice() {
    return finalPrice;
  }

  public String getResult() {
    return result;
  }

  public String getEndTime() {
    return endTime;
  }
}