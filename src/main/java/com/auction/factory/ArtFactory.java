package com.auction.factory;

import com.auction.model.*;

public class ArtFactory extends ItemFactory {

    public int a_b=0;

    @Override
    public Item create(String id, String name, double price) {
        return new Art(id, name, price); // Gọi đến class Art.java trong model của bạn
    }
}
