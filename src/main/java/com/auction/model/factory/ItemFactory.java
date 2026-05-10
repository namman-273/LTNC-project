package com.auction.model.factory;

import com.auction.model.entities.item.Item;

/**
 * abstract class.
 */
public abstract class ItemFactory {

  public abstract Item create(String id, String name, double price);
}