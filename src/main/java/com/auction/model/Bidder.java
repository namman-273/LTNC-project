package com.auction.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    public Bidder(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Bidder] ID: " + id + ", Name: " + username);
    }
    public String getName(){return username;}
}
