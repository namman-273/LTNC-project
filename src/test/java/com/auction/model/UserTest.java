package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;
import com.auction.util.core.SecurityUtils;

import org.junit.jupiter.api.Test;
 
public class UserTest {
 
  // Tạo user với password đã hash đúng cách như UserManager làm
  private static Bidder createBidder(String username, String rawPassword) {
    return new Bidder(username, SecurityUtils.hashPassword(rawPassword, username, null));
  }
 
  private static Seller createSeller(String username, String rawPassword) {
    return new Seller(username, SecurityUtils.hashPassword(rawPassword, username, null));
  }
 
  private static Admin createAdmin(String username, String rawPassword) {
    return new Admin(username, SecurityUtils.hashPassword(rawPassword, username, null));
  }
 
  // --- role ---
 
  @Test
  void bidderGetRoleReturnsBidder() {
    assertEquals("BIDDER", new Bidder("alice", "pw", null).getRole());
  }
 
  @Test
  void sellerGetRoleReturnsSeller() {
    assertEquals("SELLER", new Seller("bob", "pw", null).getRole());
  }
 
  @Test
  void adminGetRoleReturnsAdmin() {
    assertEquals("ADMIN", new Admin("carol", "pw", null).getRole());
  }
 
  // --- username / id ---
 
  @Test
  void getUsernameReturnsCorrectUsername() {
    assertEquals("dave", new Bidder("dave", "pw", null).getUsername());
  }
 
  @Test
  void getIdEqualsUsername() {
    assertEquals("eve", new Bidder("eve", "pw", null).getId());
  }
 
  @Test
  void getPasswordReturnsStoredPassword() {
    // User.getPassword() không public; kiểm tra password đã được lưu qua checkPassword
    assertTrue(new Bidder("frank", "pw", null).checkPassword("pw"));
  }
 
  // --- instanceof ---
 
  @Test
  void bidderIsInstanceOfUser() {
    assertInstanceOf(User.class, new Bidder("grace", "pw", null));
  }
 
  @Test
  void sellerIsInstanceOfUser() {
    assertInstanceOf(User.class, new Seller("henry", "pw", null));
  }
 
  @Test
  void adminIsInstanceOfUser() {
    assertInstanceOf(User.class, new Admin("ivan", "pw", null));
  }
 
  // --- checkPassword (phải hash đúng) ---
 
  
 
  @Test
  void bidderDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("mia", "pw", null).displayInfo());
  }
 
  @Test
  void sellerDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Seller("noah", "pw", null).displayInfo());
  }
 
  @Test
  void adminDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Admin("oscar", "pw", null).displayInfo());
  }
 
  // --- update ---
 
  
 
  // --- toString ---
 
  @Test
  void toStringContainsUsername() {
    assertTrue(new Bidder("quinn", "pw", null).toString().contains("quinn"));
  }
 
  @Test
  void toStringContainsRole() {
    assertTrue(new Bidder("rose", "pw", null).toString().contains("BIDDER"));
  }
 
  // --- Bidder.getName ---
 
  @Test
  void bidderGetNameReturnsUsername() {
    Bidder bidder = new Bidder("sam", "pw", null);
    assertEquals("sam", bidder.getName());
  }
}
