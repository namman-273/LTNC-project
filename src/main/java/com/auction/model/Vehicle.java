package com.auction.model;

import java.io.Serializable;

public class Vehicle extends Item implements Serializable {
    private static final long serialVersionUID = 1L;
    public Vehicle(String id, String name, double price) {
        super(id, name, price);
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Vehicle] "+itemName+" - Mẫu:");
        System.out.println("Giá khởi điểm:"+getStartingPrice());
    }
}
