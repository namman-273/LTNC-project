package com.auction.model;

/**
 * Admin class representing an administrator user in the auction system.
 */
public class Admin extends User {
  private static final long serialVersionUID = 1L;

  public Admin(String username, String password) {
    super(username, password, "ADMIN");
  }

  @Override
  public void displayInfo() {
    System.out.println("[com.auction.model.Admin] ID: " + id + ", Name: " + username);
  }
}
