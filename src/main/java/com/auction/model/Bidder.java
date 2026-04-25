package com.auction.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String username, String password) {
        super(username, password, "BIDDER");
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Bidder] ID: " + id + ", Name: " + username);
    }

    public String getName() {
        return username;
    }
}
