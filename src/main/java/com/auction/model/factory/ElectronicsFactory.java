package com.auction.model.factory;

import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.item.Item;

/**
 * elec factory khởi tạo chi tiết elec.
 */
public class ElectronicsFactory extends ItemFactory {
  @Override
  public Item create(String id, String name, double price) {
    return new Electronics(id, name, price); // Gọi đến Electronics.java
  }
}
