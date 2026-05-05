package com.auction.util;

/**
 * Manages the current user's login session (username, password, role).
 *
 * <p>Implemented as a lazy-initialized singleton. Calling {@link #clear()}
 * destroys the instance so a fresh session can be created on next login.
 */
public class SessionManager {

  private static SessionManager instance;
  private String username;
  private String password;
  private String role;

  private SessionManager() {
  }

  public static SessionManager getInstance() {
    if (instance == null) {
      instance = new SessionManager();
    }
    return instance;
  }

  public void setSession(String username, String password, String role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getRole() {
    return role;
  }

  public void clear() {
    username = null;
    password = null;
    role = null;
    instance = null;
  }
}
