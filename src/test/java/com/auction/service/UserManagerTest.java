package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class UserManagerTest {
 
  private UserManager manager;
 
  @BeforeEach
  void setUp() throws Exception {
    // Reset Singleton trước mỗi test để tránh dữ liệu rò rỉ sang test khác
    Field field = UserManager.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
    manager = UserManager.getInstance();
  }
 
  // ----------------------------------------------------------------
  // Singleton
  // ----------------------------------------------------------------
 
  @Test
  void testGetInstance_calledTwice_returnsSameObject() {
    UserManager first = UserManager.getInstance();
    UserManager second = UserManager.getInstance();
    assertSame(first, second);
  }
 
  // ----------------------------------------------------------------
  // Tài khoản admin mặc định (tạo trong constructor)
  // ----------------------------------------------------------------
 
  @Test
  void testDefaultAdmin_existsAfterInit() {
    assertNotNull(manager.findUserById("admin"));
  }
 
  @Test
  void testDefaultAdmin_canLoginWithDefaultCredentials() {
    User admin = manager.login("admin", "admin123");
    assertNotNull(admin);
  }
 
  @Test
  void testDefaultAdmin_hasAdminRole() {
    User admin = manager.findUserById("admin");
    assertInstanceOf(Admin.class, admin);
  }
 
  // ----------------------------------------------------------------
  // register
  // ----------------------------------------------------------------
 
  @Test
  void testRegister_newUsername_returnsTrue() {
    assertTrue(manager.register("alice", "pw123", "BIDDER"));
  }
 
  @Test
  void testRegister_duplicateUsername_returnsFalse() {
    manager.register("bob", "pw", "BIDDER");
    assertFalse(manager.register("bob", "other", "SELLER"));
  }
 
  @Test
  void testRegister_roleAdmin_createsAdminInstance() {
    manager.register("newadmin", "pw", "ADMIN");
    assertInstanceOf(Admin.class, manager.findUserById("newadmin"));
  }
 
  @Test
  void testRegister_roleSeller_createsSellerInstance() {
    manager.register("seller1", "pw", "SELLER");
    assertInstanceOf(Seller.class, manager.findUserById("seller1"));
  }
 
  @Test
  void testRegister_roleBidder_createsBidderInstance() {
    manager.register("bidder1", "pw", "BIDDER");
    assertInstanceOf(Bidder.class, manager.findUserById("bidder1"));
  }
 
  @Test
  void testRegister_unknownRole_createsBidderByDefault() {
    manager.register("guest1", "pw", "GUEST");
    assertInstanceOf(Bidder.class, manager.findUserById("guest1"));
  }
 
  @Test
  void testRegister_roleIsCaseInsensitive_sellerLowercase() {
    manager.register("seller2", "pw", "seller");
    assertInstanceOf(Seller.class, manager.findUserById("seller2"));
  }
 
  // ----------------------------------------------------------------
  // login
  // ----------------------------------------------------------------
 
  @Test
  void testLogin_correctPassword_returnsUserObject() {
    manager.register("carol", "secret", "BIDDER");
    assertNotNull(manager.login("carol", "secret"));
  }
 
  @Test
  void testLogin_wrongPassword_returnsNull() {
    manager.register("dave", "correct", "BIDDER");
    assertNull(manager.login("dave", "wrong"));
  }
 
  @Test
  void testLogin_nonExistentUsername_returnsNull() {
    assertNull(manager.login("ghost", "pw"));
  }
 
  @Test
  void testLogin_correctPassword_returnsUserWithCorrectUsername() {
    manager.register("eve", "pw", "SELLER");
    User user = manager.login("eve", "pw");
    assertNotNull(user);
    assertTrue(user.getUsername().equals("eve"));
  }
 
  // ----------------------------------------------------------------
  // findUserById
  // ----------------------------------------------------------------
 
  @Test
  void testFindUserById_existingId_returnsUser() {
    manager.register("frank", "pw", "BIDDER");
    assertNotNull(manager.findUserById("frank"));
  }
 
  @Test
  void testFindUserById_nonExistentId_returnsNull() {
    assertNull(manager.findUserById("nobody"));
  }
}
 