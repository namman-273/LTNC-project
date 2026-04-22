package com.auction.model;

import java.io.Serializable;

public class Electronics extends Item implements Serializable{


    public Electronics(String id, String name, double price) {
        super(id, name, price);
        
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Electronics] " + itemName);
        System.out.println("Giá khởi điểm:"+getStartingPrice());
    }
}
