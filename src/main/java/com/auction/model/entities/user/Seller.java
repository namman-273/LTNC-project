package com.auction.model.entities.user;

/**
 *  * .
 *  
 */
public class Seller extends User {
  private static final long serialVersionUID = 1L;

  public Seller(String username, String password, String email) {
    super(username, password, "SELLER", email);

  }

  @Override
  public void displayInfo() {
    System.out.println("[com.auction.model.Seller] ID: " + id + ", Name: " + username);
  }
}
