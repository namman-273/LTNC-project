package com.auction.util;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
 
import org.junit.jupiter.api.Test;
 
public class SecurityUtilsTest {
 
  @Test
  void hashPasswordReturnsNotNull() {
    assertNotNull(SecurityUtils.hashPassword("password", "user1"));
  }
 
  @Test
  void hashPasswordSameInputReturnsSameHash() {
    String hash1 = SecurityUtils.hashPassword("secret", "alice");
    String hash2 = SecurityUtils.hashPassword("secret", "alice");
    assertEquals(hash1, hash2);
  }
 
  @Test
  void hashPasswordDifferentPasswordReturnsDifferentHash() {
    String hash1 = SecurityUtils.hashPassword("password1", "alice");
    String hash2 = SecurityUtils.hashPassword("password2", "alice");
    assertNotEquals(hash1, hash2);
  }
 
  @Test
  void hashPasswordDifferentSaltReturnsDifferentHash() {
    String hash1 = SecurityUtils.hashPassword("secret", "alice");
    String hash2 = SecurityUtils.hashPassword("secret", "bob");
    assertNotEquals(hash1, hash2);
  }
 
  @Test
  void hashPasswordNullPasswordReturnsNull() {
    assertNull(SecurityUtils.hashPassword(null, "alice"));
  }
 
  @Test
  void hashPasswordNullSaltReturnsNull() {
    assertNull(SecurityUtils.hashPassword("pw", null));
  }
 
  @Test
  void hashPasswordEmptyPasswordReturnsNotNull() {
    assertNotNull(SecurityUtils.hashPassword("", "alice"));
  }
 
  @Test
  void hashPasswordResultIsBase64Encoded() {
    String hash = SecurityUtils.hashPassword("pw", "user");
    // Base64 chỉ chứa ký tự chữ, số, +, /, =
    assertTrue(hash.matches("^[A-Za-z0-9+/=]+$"));
  }
 
  private void assertTrue(boolean condition) {
    org.junit.jupiter.api.Assertions.assertTrue(condition);
  }
}