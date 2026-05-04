package com.auction.model;

import com.auction.factory.*;

public class CreateItem {
    public static ItemFactory getFactory(String type) {
        // FIX [NeedBraces]: thêm {} cho if một dòng
        if (type.equalsIgnoreCase("ART")) {
            return new ArtFactory();
        }
        if (type.equalsIgnoreCase("ELECTRONICS")) {
            return new ElectronicsFactory();
        }
        return new VehicleFactory();
    }
}
