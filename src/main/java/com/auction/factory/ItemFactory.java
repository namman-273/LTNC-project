package com.auction.factory;

import com.auction.model.Item;

public abstract class ItemFactory {

    public abstract Item create(String id, String name, double price);
}