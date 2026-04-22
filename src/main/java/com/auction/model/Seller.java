package com.auction.model;

import java.io.Serializable;

public class Seller extends User implements Serializable{
    public Seller(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Seller] ID: " + id + ", Name: " + username);
    }
}
