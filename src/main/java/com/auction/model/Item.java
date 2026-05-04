package com.auction.model;

public abstract class Item extends Entity {
  protected String itemName;
  private double startingPrice;
  private volatile double currentPrice;
  public String highestBidder;
  private static final long serialVersionUID = 1L;

  public Item(String id, String itemName, double startingPrice) {
    super(id);
    this.itemName = itemName;
    this.startingPrice = startingPrice;
    this.currentPrice = startingPrice;
    this.highestBidder = "No bids yet";
  }

  public String getItemName() {
    return itemName;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public synchronized void setCurrentPrice(double currentPrice) {
    this.currentPrice = currentPrice;
  }

  public double getCurrentPrice() {
    return currentPrice;
  }

  public String getHighestBidder() {
    return highestBidder;
  }

  public synchronized void setHighestBidder(String highestBidder) {
    this.highestBidder = highestBidder;
  }
}
