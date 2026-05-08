package com.auction.factory;

import com.auction.model.Electronics;
import com.auction.model.Item;

/**
 * elec factory khởi tạo chi tiết elec.
 */
public class ElectronicsFactory extends ItemFactory {
  @Override
  public Item create(String id, String name, double price) {
    return new Electronics(id, name, price); // Gọi đến Electronics.java
  }
}
