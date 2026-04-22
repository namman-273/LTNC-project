package com.auction.model;

//To-do:chuyen design pattern sang factory method
public class CreateItem {
    public static Item createItem(String type, String id, String name, double price) {
        if (type == null) return null;

        switch (type.toLowerCase()) {
            case "electronics":
            
                return new Electronics(id, name, price);
            case "art":
                return new Art(id, name, price);
            case "vehicle":
            
                return new Vehicle(id, name, price);
            default:

                throw new IllegalArgumentException("Loại sản phẩm '" + type + "' không hợp lệ!");
        }
    }
}
