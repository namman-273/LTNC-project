package com.auction.model;

import java.io.Serializable;

public class Art extends Item implements Serializable{
   

    public Art(String id, String name, double price) {
        super(id, name, price);
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Art] " + itemName);
        System.out.println("Giá khởi điểm:"+getStartingPrice());
    }
}
