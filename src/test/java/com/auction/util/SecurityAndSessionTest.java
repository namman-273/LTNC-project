package com.auction.util;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.util.core.SecurityUtils;
import com.auction.util.core.SessionManager;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SecurityUtils: hashPassword (hợp lệ, null password, null salt, deterministic, salted).
 * SessionManager: setSession, getters, clear, singleton.
 */
public class SecurityAndSessionTest {

    // ==================== SecurityUtils ====================

    @Test
    void hashPasswordReturnsNonNullForValidInputs() {
        assertNotNull(SecurityUtils.hashPassword("password", "username"));
    }

    @Test
    void hashPasswordIsDeterministic() {
        String h1 = SecurityUtils.hashPassword("secret", "alice");
        String h2 = SecurityUtils.hashPassword("secret", "alice");
        assertEquals(h1, h2);
    }

    @Test
    void hashPasswordDifferentPasswordsDifferentHashes() {
        String h1 = SecurityUtils.hashPassword("pass1", "alice");
        String h2 = SecurityUtils.hashPassword("pass2", "alice");
        assertNotEquals(h1, h2);
    }

    @Test
    void hashPasswordDifferentSaltsDifferentHashes() {
        // Same password, different usernames (salts) → different hash
        String h1 = SecurityUtils.hashPassword("secret", "alice");
        String h2 = SecurityUtils.hashPassword("secret", "bob");
        assertNotEquals(h1, h2, "Same password with different salts must produce different hashes");
    }

    @Test
    void hashPasswordNullPasswordReturnsNull() {
        assertNull(SecurityUtils.hashPassword(null, "alice"));
    }

    @Test
    void hashPasswordNullSaltReturnsNull() {
        assertNull(SecurityUtils.hashPassword("password", null));
    }

    @Test
    void hashPasswordBothNullReturnsNull() {
        assertNull(SecurityUtils.hashPassword(null, null));
    }

    @Test
    void hashPasswordEmptyStringsDoNotThrow() {
        assertDoesNotThrow(() -> SecurityUtils.hashPassword("", ""));
    }

    @Test
    void hashPasswordProducesBase64String() {
        String hash = SecurityUtils.hashPassword("pw", "user");
        assertNotNull(hash);
        // Base64 strings only contain A-Z, a-z, 0-9, +, /, =
        assertTrue(hash.matches("^[A-Za-z0-9+/=]+$"),
            "Hash should be a valid Base64 string");
    }

    // ==================== SessionManager ====================

    @BeforeEach
    void clearSession() throws Exception {
        Field f = SessionManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void sessionManagerSingletonReturnsSameInstance() {
        assertSame(SessionManager.getInstance(), SessionManager.getInstance());
    }

    @Test
    void setSessionStoresUsername() {
        SessionManager.getInstance().setSession("alice", "pw", "BIDDER");
        assertEquals("alice", SessionManager.getInstance().getUsername());
    }

    @Test
    void setSessionStoresPassword() {
        SessionManager.getInstance().setSession("alice", "secret", "BIDDER");
        assertEquals("secret", SessionManager.getInstance().getPassword());
    }

    @Test
    void setSessionStoresRole() {
        SessionManager.getInstance().setSession("alice", "pw", "ADMIN");
        assertEquals("ADMIN", SessionManager.getInstance().getRole());
    }

    @Test
    void clearResetsUsername() {
        SessionManager sm = SessionManager.getInstance();
        sm.setSession("alice", "pw", "BIDDER");
        sm.clear();
        // After clear, getInstance creates a new one
        assertNull(SessionManager.getInstance().getUsername());
    }

    @Test
    void clearResetsRole() {
        SessionManager sm = SessionManager.getInstance();
        sm.setSession("alice", "pw", "BIDDER");
        sm.clear();
        assertNull(SessionManager.getInstance().getRole());
    }

    @Test
    void clearResetsPassword() {
        SessionManager sm = SessionManager.getInstance();
        sm.setSession("alice", "pw", "BIDDER");
        sm.clear();
        assertNull(SessionManager.getInstance().getPassword());
    }

    @Test
    void freshSessionHasNullUsername() {
        assertNull(SessionManager.getInstance().getUsername());
    }

    @Test
    void sessionCanBeOverwritten() {
        SessionManager sm = SessionManager.getInstance();
        sm.setSession("alice", "pw1", "BIDDER");
        sm.setSession("bob", "pw2", "SELLER");
        assertEquals("bob", sm.getUsername());
        assertEquals("SELLER", sm.getRole());
    }
}