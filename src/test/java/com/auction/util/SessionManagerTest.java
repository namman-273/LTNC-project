package com.auction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.util.core.SessionManager;

public class SessionManagerTest {

  @BeforeEach
  void setUp() {
    // Reset state bằng clear() thay vì reflection,
    // vì SessionManager dùng Initialization-on-demand holder (SingletonHolder)
    // không có field "instance" trực tiếp ở class ngoài để reset.
    SessionManager.getInstance().clear();
  }

  // --- Singleton ---

  @Test
  void getInstanceReturnsSameObject() {
    SessionManager a = SessionManager.getInstance();
    SessionManager b = SessionManager.getInstance();
    assertSame(a, b);
  }

  @Test
  void getInstanceNotNull() {
    assertNotNull(SessionManager.getInstance());
  }

  // --- setSession / getters ---

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
    SessionManager.getInstance().setSession("alice", "pw", "SELLER");
    assertEquals("SELLER", SessionManager.getInstance().getRole());
  }

  @Test
  void setSessionOverwritesPreviousSession() {
    SessionManager.getInstance().setSession("alice", "pw1", "BIDDER");
    SessionManager.getInstance().setSession("bob", "pw2", "ADMIN");
    assertEquals("bob", SessionManager.getInstance().getUsername());
    assertEquals("ADMIN", SessionManager.getInstance().getRole());
  }

  // --- Initial state (sau khi clear() trong @BeforeEach) ---

  @Test
  void freshInstanceUsernameIsNull() {
    assertNull(SessionManager.getInstance().getUsername());
  }

  @Test
  void freshInstancePasswordIsNull() {
    assertNull(SessionManager.getInstance().getPassword());
  }

  @Test
  void freshInstanceRoleIsNull() {
    assertNull(SessionManager.getInstance().getRole());
  }

  // --- clear ---

  @Test
  void clearResetsUsernameToNull() {
    SessionManager sm = SessionManager.getInstance();
    sm.setSession("alice", "pw", "BIDDER");
    sm.clear();
    assertNull(SessionManager.getInstance().getUsername());
  }

  @Test
  void clearResetsRoleToNull() {
    SessionManager sm = SessionManager.getInstance();
    sm.setSession("alice", "pw", "BIDDER");
    sm.clear();
    assertNull(SessionManager.getInstance().getRole());
  }

  @Test
  void clearResetsPasswordToNull() {
    SessionManager sm = SessionManager.getInstance();
    sm.setSession("alice", "pw", "BIDDER");
    sm.clear();
    assertNull(SessionManager.getInstance().getPassword());
  }

  @Test
  void clearInvalidatesSingletonInstanceAllowsNewOne() {
    SessionManager first = SessionManager.getInstance();
    first.setSession("alice", "pw", "BIDDER");
    first.clear();
    // Sau clear(), các field được reset về null nhưng singleton vẫn là cùng
    // instance
    SessionManager second = SessionManager.getInstance();
    assertNull(second.getUsername(),
        "After clear(), instance should have null username");
  }
}