package com.auction.model;

import com.auction.factory.*;

public class CreateItem {
    public static ItemFactory getFactory(String type) {
        if (type.equalsIgnoreCase("ART"))
            return new ArtFactory();
        if (type.equalsIgnoreCase("ELECTRONICS"))
            return new ElectronicsFactory();
        return new VehicleFactory();
    }
}
