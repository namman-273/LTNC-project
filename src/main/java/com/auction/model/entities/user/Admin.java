package com.auction.model.entities.user;

/**
 * Admin class representing an administrator user in the auction system.
 */
public class Admin extends User {
  private static final long serialVersionUID = 1L;

  public Admin(String username, String password, String email) {
    super(username, password, "ADMIN", email);
  }

 
}
