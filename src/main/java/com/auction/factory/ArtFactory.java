package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Item;

/**
 * Art factory khởi tạo chi tiết Art.
 */
public class ArtFactory extends ItemFactory {

  @Override
  public Item create(String id, String name, double price) {
    return new Art(id, name, price); // Gọi đến class Art.java trong model của bạn
  }
}
