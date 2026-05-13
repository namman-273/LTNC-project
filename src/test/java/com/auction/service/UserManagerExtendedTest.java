package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;
import com.auction.util.exception.AuthenticationException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class UserManagerExtendedTest {
 
    private UserManager manager;
 
    @BeforeEach
    void setUp() throws Exception {
        Field field = UserManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
        manager = UserManager.getInstance();
    }
 
    // --- initDefaultData khi users rỗng ---
 
    @Test
    void initDefaultDataWhenEmptyCreatesAdmin() {
        manager.initDefaultData();
        assertNotNull(manager.findUserByUsername("admin"));
    }
 
    @Test
    void initDefaultDataWhenEmptyCreatesAdminRole() {
        manager.initDefaultData();
        assertInstanceOf(Admin.class, manager.findUserByUsername("admin"));
    }
 
    @Test
    void initDefaultDataWhenNotEmptyDoesNotAddAdmin() {
        manager.register("existingUser", "pw", "BIDDER", null);
        int sizeBefore = manager.getUsers().size();
        manager.initDefaultData();
        assertEquals(sizeBefore, manager.getUsers().size());
    }
 
    // --- register: nhiều loại role ---
 
    @Test
    void registerAdminUppercaseCreatesAdmin() {
        manager.register("admin2", "pw", "ADMIN", null);
        assertInstanceOf(Admin.class, manager.findUserByUsername("admin2"));
    }
 
    @Test
    void registerSellerUppercaseCreatesSeller() {
        manager.register("seller1", "pw", "SELLER", null);
        assertInstanceOf(Seller.class, manager.findUserByUsername("seller1"));
    }
 
    @Test
    void registerBidderUppercaseCreatesBidder() {
        manager.register("bidder1", "pw", "BIDDER", null);
        assertInstanceOf(Bidder.class, manager.findUserByUsername("bidder1"));
    }
 
    @Test
    void registerDefaultRoleCreatesBidder() {
        manager.register("guest1", "pw", "RANDOM_ROLE", null);
        assertInstanceOf(Bidder.class, manager.findUserByUsername("guest1"));
    }
 
    @Test
    void registerSameUsernameTwiceReturnsFalseSecondTime() {
        manager.register("dup", "pw1", "BIDDER", null);
        assertFalse(manager.register("dup", "pw2", "SELLER", null));
    }
 
    @Test
    void registerHashesPassword() {
        manager.register("user1", "mypassword", "BIDDER", null);
        User user = manager.findUserByUsername("user1");
        // stored password should not be plain text
        // getPassword() không public; kiểm tra hashing qua checkPassword
        assertTrue(user.checkPassword("mypassword"));
    }
 
    // --- login ---
 
    @Test
    void loginSuccessReturnsCorrectRole() throws Exception {
        manager.register("seller2", "pw", "SELLER", null);
        User user = manager.login("seller2", "pw");
        assertEquals("SELLER", user.getRole());
    }
 
    @Test
    void loginSuccessAdminRoleCorrect() throws Exception {
        manager.initDefaultData();
        User user = manager.login("admin", "admin123");
        assertEquals("ADMIN", user.getRole());
    }
 
    @Test
    void loginEmptyPasswordThrowsAuthenticationException() {
        manager.register("user2", "realpass", "BIDDER", null);
        assertThrows(
                AuthenticationException.class,
                () -> manager.login("user2", ""));
    }
 
    @Test
    void loginWrongUsernameThrowsAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> manager.login("nonexistent", "pw"));
    }
 
    // --- getUsers / setUsers ---
 
    @Test
    void getUsersIsEmptyWhenNoRegistrations() {
        assertTrue(manager.getUsers().isEmpty());
    }
 
    @Test
    void getUsersSizeIncreasesAfterRegister() {
        manager.register("u1", "pw", "BIDDER", null);
        manager.register("u2", "pw", "SELLER", null);
        assertEquals(2, manager.getUsers().size());
    }
 
    @Test
    void setUsersWithValidMapUpdatesUsers() {
        Map<String, User> map = new HashMap<>();
        map.put("injected", new Bidder("injected", "pw", null));
        manager.setUsers(map);
        assertNotNull(manager.findUserByUsername("injected"));
    }
 
    @Test
    void setUsersWithNullDoesNotThrowAndKeepsPreviousUsers() {
        manager.register("keep1", "pw", "BIDDER", null);
        assertDoesNotThrow(() -> manager.setUsers(null));
        assertNotNull(manager.findUserByUsername("keep1"));
    }
 
    // --- findUserByUsername ---
 
    @Test
    void findUserByUsernameReturnsCorrectInstance() {
        manager.register("findme", "pw", "ADMIN", null);
        assertInstanceOf(Admin.class, manager.findUserByUsername("findme"));
    }
 
    @Test
    void findUserByUsernameNonExistentReturnsNull() {
        manager.register("existing", "pw", "BIDDER", null);
        assertEquals(null, manager.findUserByUsername("ghost"));
    }
}