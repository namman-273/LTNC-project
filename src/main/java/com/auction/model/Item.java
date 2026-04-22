package com.auction.model;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable{
    protected String itemName;
    private double startingPrice;
    private volatile double currentPrice;
    public String highestBidder;

    public Item(String id, String itemName, double startingPrice) {
        super(id);
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.currentPrice=startingPrice;
        this.highestBidder="No bids yet";
    }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice;}
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
