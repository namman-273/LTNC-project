package com.auction.model;

import java.util.Map;

public class CreateItem {
    public static Item createItem(String type, String id, String name, double price, Map<String, Object> details) {
        if (type == null) return null;

        switch (type.toLowerCase()) {
            case "electronics":
                Object value = details.get("warranty");
                int warranty = (value instanceof Integer) ? (Integer) value : 0;
                return new Electronics(id, name, price,warranty);
            case "art":
                return new Art(id, name, price);
            case "vehicle":
                Object value3 = details.get("model");
                int model = (value3 instanceof Integer) ? (Integer) value3 : 0;
                return new Vehicle(id, name, price,model);
            default:

                throw new IllegalArgumentException("Loại sản phẩm '" + type + "' không hợp lệ!");
        }
    }
}
