package com.auction.model.factory;

import com.auction.model.entities.item.Item;
import com.auction.model.entities.item.Vehicle;

/**
 * .
 */
public class VehicleFactory extends ItemFactory {
  @Override
  public Item create(String id, String name, double price) {
    return new Vehicle(id, name, price);
  }
}