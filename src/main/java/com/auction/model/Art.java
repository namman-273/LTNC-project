package com.auction.model;

public class Art extends Item {
   

    public Art(String id, String name, double price) {
        super(id, name, price);
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Art] " + itemName);
        System.out.println("Giá khởi điểm:"+getStartingPrice());
    }
}
