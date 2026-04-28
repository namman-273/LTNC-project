package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.util.SecurityUtils;
import org.junit.jupiter.api.Test;
 
public class UserTest {
 
  // Tạo user với password đã hash đúng cách như UserManager làm
  private static Bidder createBidder(String username, String rawPassword) {
    return new Bidder(username, SecurityUtils.hashPassword(rawPassword, username));
  }
 
  private static Seller createSeller(String username, String rawPassword) {
    return new Seller(username, SecurityUtils.hashPassword(rawPassword, username));
  }
 
  private static Admin createAdmin(String username, String rawPassword) {
    return new Admin(username, SecurityUtils.hashPassword(rawPassword, username));
  }
 
  // --- role ---
 
  @Test
  void bidderGetRoleReturnsBidder() {
    assertEquals("BIDDER", new Bidder("alice", "pw").getRole());
  }
 
  @Test
  void sellerGetRoleReturnsSeller() {
    assertEquals("SELLER", new Seller("bob", "pw").getRole());
  }
 
  @Test
  void adminGetRoleReturnsAdmin() {
    assertEquals("ADMIN", new Admin("carol", "pw").getRole());
  }
 
  // --- username / id ---
 
  @Test
  void getUsernameReturnsCorrectUsername() {
    assertEquals("dave", new Bidder("dave", "pw").getUsername());
  }
 
  @Test
  void getIdEqualsUsername() {
    assertEquals("eve", new Bidder("eve", "pw").getId());
  }
 
  @Test
  void getPasswordReturnsStoredPassword() {
    assertNotNull(new Bidder("frank", "pw").getPassword());
  }
 
  // --- instanceof ---
 
  @Test
  void bidderIsInstanceOfUser() {
    assertInstanceOf(User.class, new Bidder("grace", "pw"));
  }
 
  @Test
  void sellerIsInstanceOfUser() {
    assertInstanceOf(User.class, new Seller("henry", "pw"));
  }
 
  @Test
  void adminIsInstanceOfUser() {
    assertInstanceOf(User.class, new Admin("ivan", "pw"));
  }
 
  // --- checkPassword (phải hash đúng) ---
 
  @Test
  void checkPasswordCorrectReturnsTrue() {
    Bidder bidder = createBidder("jack", "mypassword");
    assertTrue(bidder.checkPassword("mypassword"));
  }
 
  @Test
  void checkPasswordWrongReturnsFalse() {
    Bidder bidder = createBidder("kate", "correct");
    assertFalse(bidder.checkPassword("wrong"));
  }
 
  @Test
  void checkPasswordEmptyStringDoesNotThrow() {
    Bidder bidder = createBidder("leo", "pw");
    assertDoesNotThrow(() -> bidder.checkPassword(""));
  }
 
  // --- displayInfo ---
 
  @Test
  void bidderDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("mia", "pw").displayInfo());
  }
 
  @Test
  void sellerDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Seller("noah", "pw").displayInfo());
  }
 
  @Test
  void adminDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Admin("oscar", "pw").displayInfo());
  }
 
  // --- update ---
 
  @Test
  void updateDoesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("paul", "pw").update("TEST_MSG"));
  }
 
  // --- toString ---
 
  @Test
  void toStringContainsUsername() {
    assertTrue(new Bidder("quinn", "pw").toString().contains("quinn"));
  }
 
  @Test
  void toStringContainsRole() {
    assertTrue(new Bidder("rose", "pw").toString().contains("BIDDER"));
  }
 
  // --- Bidder.getName ---
 
  @Test
  void bidderGetNameReturnsUsername() {
    Bidder bidder = new Bidder("sam", "pw");
    assertEquals("sam", bidder.getName());
  }
}
