package com.auction.model;

import java.io.Serializable;

public class Admin extends User implements Serializable {
    public Admin(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Admin] ID: " + id + ", Name: " + username);
    }
}
