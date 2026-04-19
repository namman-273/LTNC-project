package com.auction.model;

public class Seller extends User {
    public Seller(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Seller] ID: " + id + ", Name: " + username);
    }
}
