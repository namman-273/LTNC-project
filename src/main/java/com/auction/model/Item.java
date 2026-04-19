package com.auction.model;

public abstract class Item extends Entity {
    protected String itemName;
    protected double startingPrice;
    private volatile double currentPrice;

    public Item(String id, String itemName, double startingPrice) {
        super(id);
        this.itemName = itemName;
        this.startingPrice = startingPrice;
    }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice;}
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
}
