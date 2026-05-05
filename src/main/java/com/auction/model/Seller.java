package com.auction.model;

public class Seller extends User {
  public Seller(String username, String password) {
    super(username, password, "SELLER");
  }

  private static final long serialVersionUID = 1L;

  @Override
  public void displayInfo() {
    System.out.println("[com.auction.model.Seller] ID: " + id + ", Name: " + username);
  }
}
