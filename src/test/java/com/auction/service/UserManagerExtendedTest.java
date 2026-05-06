package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.exception.AuthenticationException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
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
        manager.register("existingUser", "pw", "BIDDER");
        int sizeBefore = manager.getUsers().size();
        manager.initDefaultData();
        assertEquals(sizeBefore, manager.getUsers().size());
    }
 
    // --- register: nhiều loại role ---
 
    @Test
    void registerAdminUppercaseCreatesAdmin() {
        manager.register("admin2", "pw", "ADMIN");
        assertInstanceOf(Admin.class, manager.findUserByUsername("admin2"));
    }
 
    @Test
    void registerSellerUppercaseCreatesSeller() {
        manager.register("seller1", "pw", "SELLER");
        assertInstanceOf(Seller.class, manager.findUserByUsername("seller1"));
    }
 
    @Test
    void registerBidderUppercaseCreatesBidder() {
        manager.register("bidder1", "pw", "BIDDER");
        assertInstanceOf(Bidder.class, manager.findUserByUsername("bidder1"));
    }
 
    @Test
    void registerDefaultRoleCreatesBidder() {
        manager.register("guest1", "pw", "RANDOM_ROLE");
        assertInstanceOf(Bidder.class, manager.findUserByUsername("guest1"));
    }
 
    @Test
    void registerSameUsernameTwiceReturnsFalseSecondTime() {
        manager.register("dup", "pw1", "BIDDER");
        assertFalse(manager.register("dup", "pw2", "SELLER"));
    }
 
    @Test
    void registerHashesPassword() {
        manager.register("user1", "mypassword", "BIDDER");
        User user = manager.findUserByUsername("user1");
        // stored password should not be plain text
        assertFalse("mypassword".equals(user.getPassword()));
    }
 
    // --- login ---
 
    @Test
    void loginSuccessReturnsCorrectRole() throws Exception {
        manager.register("seller2", "pw", "SELLER");
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
        manager.register("user2", "realpass", "BIDDER");
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
        manager.register("u1", "pw", "BIDDER");
        manager.register("u2", "pw", "SELLER");
        assertEquals(2, manager.getUsers().size());
    }
 
    @Test
    void setUsersWithValidMapUpdatesUsers() {
        Map<String, User> map = new HashMap<>();
        map.put("injected", new Bidder("injected", "pw"));
        manager.setUsers(map);
        assertNotNull(manager.findUserByUsername("injected"));
    }
 
    @Test
    void setUsersWithNullDoesNotThrowAndKeepsPreviousUsers() {
        manager.register("keep1", "pw", "BIDDER");
        assertDoesNotThrow(() -> manager.setUsers(null));
        assertNotNull(manager.findUserByUsername("keep1"));
    }
 
    // --- findUserByUsername ---
 
    @Test
    void findUserByUsernameReturnsCorrectInstance() {
        manager.register("findme", "pw", "ADMIN");
        assertInstanceOf(Admin.class, manager.findUserByUsername("findme"));
    }
 
    @Test
    void findUserByUsernameNonExistentReturnsNull() {
        manager.register("existing", "pw", "BIDDER");
        assertEquals(null, manager.findUserByUsername("ghost"));
    }
}