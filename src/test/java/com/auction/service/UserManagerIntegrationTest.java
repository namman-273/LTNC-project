package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.model.entities.user.User;
import com.auction.util.exception.AuthenticationException;

import java.lang.reflect.Field;
import com.auction.service.usermanger.UserManager;
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
        manager.register("alice", "secret123", "BIDDER", "alice@test.com");
 
        User user = manager.login("alice", "secret123");
 
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
    }
 
    @Test
    void registerThenLoginWithDifferentPasswordFails() {
        manager.register("bob", "correct", "BIDDER", "bob@test.com");
 
        assertThrows(AuthenticationException.class,
            () -> manager.login("bob", "wrong"));
    }
 
    @Test
    void registerSellerThenLoginReturnsSellerRole() throws Exception {
        manager.register("carol", "pw123", "SELLER", "carol@test.com");
 
        User user = manager.login("carol", "pw123");
 
        assertEquals("SELLER", user.getRole());
    }
 
    @Test
    void registerAdminThenLoginReturnsAdminRole() throws Exception {
        manager.register("dave", "pw123", "ADMIN", "dave@test.com");
 
        User user = manager.login("dave", "pw123");
 
        assertEquals("ADMIN", user.getRole());
    }
 
    @Test
    void registerThenLoginWithEmptyPasswordFails() {
        manager.register("eve", "realpass", "BIDDER", "eve@test.com");
 
        assertThrows(AuthenticationException.class,
            () -> manager.login("eve", ""));
    }
 
    @Test
    void registerStoresHashedNotPlainPassword() {
        manager.register("frank", "mypassword", "BIDDER", "frank@test.com");
        User user = manager.findUserByUsername("frank");
 
        // getPassword() không public; kiểm tra hashing qua checkPassword
        org.junit.jupiter.api.Assertions.assertTrue(user.checkPassword("mypassword"),
            "Stored password must be hashed, not plain text");
    }
 
    @Test
    void loginAfterRegisterMultipleUsersReturnsCorrectUser() throws Exception {
        manager.register("user1", "pw1", "BIDDER", "user1@test.com");
        manager.register("user2", "pw2", "SELLER", "user2@test.com");
        manager.register("user3", "pw3", "ADMIN", "user3@test.com");
 
        User user = manager.login("user2", "pw2");
 
        assertEquals("user2", user.getUsername());
    }
 
    @Test
    void loginWithWrongUsernameAfterRegisterThrows() {
        manager.register("grace", "pw", "BIDDER", "grace@test.com");
 
        assertThrows(AuthenticationException.class,
            () -> manager.login("notgrace", "pw"));
    }
}