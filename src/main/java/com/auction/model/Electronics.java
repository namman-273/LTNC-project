package com.auction.model;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, double price, int warranty) {
        super(id, name, price);
        this.warrantyMonths = warranty;
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Electronics] " + itemName + " - Bảo hành: " + warrantyMonths + " tháng");
        System.out.println("Giá khởi điểm:"+startingPrice);
    }
}
