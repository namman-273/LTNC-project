package com.auction.factory;

import com.auction.model.Item;
import com.auction.model.Vehicle;


public class VehicleFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, double price) {
        return new Vehicle(id, name, price);
    }
}