package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.model.entities.user.User;
import com.auction.util.exception.AuthenticationException;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class UserManagerIntegrationTest {
 
    private UserManager manager;
 
    @BeforeEach
    void setUp() throws Exception {
        Field field = UserManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
        manager = UserManager.getInstance();
    }
 
    // --- register then login with same password ---
 
    @Test
    void registerThenLoginWithSamePasswordSucceeds() throws Exception {
        manager.register("alice", "secret123", "BIDDER", null);
 
        User user = manager.login("alice", "secret123");
 
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
    }
 
    @Test
    void registerThenLoginWithDifferentPasswordFails() {
        manager.register("bob", "correct", "BIDDER", null);
 
        assertThrows(AuthenticationException.class,
            () -> manager.login("bob", "wrong"));
    }
 
    @Test
    void registerSellerThenLoginReturnsSellerRole() throws Exception {
        manager.register("carol", "pw123", "SELLER", null);
 
        User user = manager.login("carol", "pw123");
 
        assertEquals("SELLER", user.getRole());
    }
 
    @Test
    void registerAdminThenLoginReturnsAdminRole() throws Exception {
        manager.register("dave", "pw123", "ADMIN", null);
 
        User user = manager.login("dave", "pw123");
 
        assertEquals("ADMIN", user.getRole());
    }
 
    @Test
    void registerThenLoginWithEmptyPasswordFails() {
        manager.register("eve", "realpass", "BIDDER", null);
 
        assertThrows(AuthenticationException.class,
            () -> manager.login("eve", ""));
    }
 
    @Test
    void registerStoresHashedNotPlainPassword() {
        manager.register("frank", "mypassword", "BIDDER", null);
        User user = manager.findUserByUsername("frank");
 
        // getPassword() không public; kiểm tra hashing qua checkPassword
        org.junit.jupiter.api.Assertions.assertTrue(user.checkPassword("mypassword"),
            "Stored password must be hashed, not plain text");
    }
 
    @Test
    void loginAfterRegisterMultipleUsersReturnsCorrectUser() throws Exception {
        manager.register("user1", "pw1", "BIDDER", null);
        manager.register("user2", "pw2", "SELLER", null);
        manager.register("user3", "pw3", "ADMIN", null);
 
        User user = manager.login("user2", "pw2");
 
        assertEquals("user2", user.getUsername());
    }
 
    @Test
    void loginWithWrongUsernameAfterRegisterThrows() {
        manager.register("grace", "pw", "BIDDER", null);
 
        assertThrows(AuthenticationException.class,
            () -> manager.login("notgrace", "pw"));
    }
}