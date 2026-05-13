package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;
import com.auction.util.exception.AuthenticationException;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class UserManagerTest {
 
  private UserManager manager;
 
  @BeforeEach
  void setUp() throws Exception {
    Field field = UserManager.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
    manager = UserManager.getInstance();
    // initDefaultData tạo admin mặc định khi users rỗng
    manager.initDefaultData();
  }
 
  // --- Singleton ---
 
  @Test
  void getInstanceReturnsSameObject() {
    assertSame(UserManager.getInstance(), UserManager.getInstance());
  }
 
  // --- initDefaultData ---
 
  @Test
  void initDefaultDataCreatesAdminUser() {
    assertNotNull(manager.findUserByUsername("admin"));
  }
 
  @Test
  void initDefaultDataCreatesAdminInstance() {
    assertInstanceOf(Admin.class, manager.findUserByUsername("admin"));
  }
 
  @Test
  void initDefaultDataCalledTwiceDoesNotDuplicate() {
    manager.initDefaultData();
    int sizeBefore = manager.getUsers().size();
    manager.initDefaultData();
    assertEquals(sizeBefore, manager.getUsers().size());
  }
 
  // --- register ---
 
  @Test
  void registerNewUsernameReturnsTrue() {
    assertTrue(manager.register("alice", "pw123", "BIDDER", null));
  }
 
  @Test
  void registerDuplicateUsernameReturnsFalse() {
    manager.register("bob", "pw", "BIDDER", null);
    assertFalse(manager.register("bob", "other", "SELLER", null));
  }
 
  @Test
  void registerRoleAdminCreatesAdminInstance() {
    manager.register("newadmin", "pw", "ADMIN", null);
    assertInstanceOf(Admin.class, manager.findUserByUsername("newadmin"));
  }
 
  @Test
  void registerRoleSellerCreatesSellerInstance() {
    manager.register("seller1", "pw", "SELLER", null);
    assertInstanceOf(Seller.class, manager.findUserByUsername("seller1"));
  }
 
  @Test
  void registerRoleBidderCreatesBidderInstance() {
    manager.register("bidder1", "pw", "BIDDER", null);
    assertInstanceOf(Bidder.class, manager.findUserByUsername("bidder1"));
  }
 
  @Test
  void registerUnknownRoleCreatesBidderByDefault() {
    manager.register("guest1", "pw", "GUEST", null);
    assertInstanceOf(Bidder.class, manager.findUserByUsername("guest1"));
  }
 
  @Test
  void registerRoleCaseInsensitiveSellerLowercase() {
    manager.register("seller2", "pw", "seller", null);
    assertInstanceOf(Seller.class, manager.findUserByUsername("seller2"));
  }
 
  @Test
  void registerRoleCaseInsensitiveAdminLowercase() {
    manager.register("admin2", "pw", "admin", null);
    assertInstanceOf(Admin.class, manager.findUserByUsername("admin2"));
  }
 
  // --- login: thành công ---
 
  @Test
  void loginCorrectPasswordReturnsUser() throws Exception {
    manager.register("carol", "secret", "BIDDER", null);
    assertNotNull(manager.login("carol", "secret"));
  }
 
  @Test
  void loginCorrectPasswordReturnsCorrectUsername() throws Exception {
    manager.register("dave", "pw", "SELLER", null);
    User user = manager.login("dave", "pw");
    assertEquals("dave", user.getUsername());
  }
 
  @Test
  void loginAdminDefaultCredentialsReturnsAdmin() throws Exception {
    User admin = manager.login("admin", "admin123");
    assertInstanceOf(Admin.class, admin);
  }
 
  // --- login: thất bại (throws AuthenticationException) ---
 
  @Test
  void loginWrongPasswordThrowsAuthenticationException() {
    manager.register("eve", "correct", "BIDDER", null);
    assertThrows(
        AuthenticationException.class,
        () -> manager.login("eve", "wrong"));
  }
 
  @Test
  void loginNonExistentUsernameThrowsAuthenticationException() {
    assertThrows(
        AuthenticationException.class,
        () -> manager.login("ghost", "pw"));
  }
 
  // --- findUserByUsername ---
 
  @Test
  void findUserByUsernameExistingReturnsUser() {
    manager.register("frank", "pw", "BIDDER", null);
    assertNotNull(manager.findUserByUsername("frank"));
  }
 
  @Test
  void findUserByUsernameNonExistentReturnsNull() {
    assertNull(manager.findUserByUsername("nobody"));
  }
 
  // --- getUsers / setUsers ---
 
  @Test
  void getUsersNotNull() {
    assertNotNull(manager.getUsers());
  }
 
  @Test
  void getUsersContainsRegisteredUser() {
    manager.register("grace", "pw", "BIDDER", null);
    assertTrue(manager.getUsers().containsKey("grace"));
  }
 
  @Test
  void setUsersNullDoesNotThrow() {
    assertDoesNotThrow(() -> manager.setUsers(null));
  }
}