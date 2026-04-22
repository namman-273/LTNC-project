package com.auction.model;

import java.io.Serializable;

public class Bidder extends User implements Serializable{
    public Bidder(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Bidder] ID: " + id + ", Name: " + username);
    }
    public String getName(){return username;}
}
