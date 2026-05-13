package com.auction.service.usermanger;

import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;

/**
 * Factory class để tạo User objects theo role.
 * Tuân thủ Open/Closed Principle - dễ mở rộng cho role mới.
 */
public class UserFactory {

  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_SELLER = "SELLER";
  private static final String ROLE_BIDDER = "BIDDER";

  /**
   * Tạo user object dựa trên role.
   */
  public User createUser(String username, String password, String role, String email) {
    switch (role.toUpperCase()) {
      case ROLE_ADMIN:
        return new Admin(username, password, email);
      case ROLE_SELLER:
        return new Seller(username, password, email);
      case ROLE_BIDDER:
      default:
        return new Bidder(username, password, email);
    }
  }
}
