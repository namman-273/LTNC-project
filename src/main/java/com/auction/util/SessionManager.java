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

  private SessionManager() {}

  /**
   * Returns the singleton instance, creating it if necessary.
   *
   * @return the SessionManager instance
   */
  public static SessionManager getInstance() {
    if (instance == null) {
      instance = new SessionManager();
    }
    return instance;
  }

  /**
   * Stores session credentials after a successful login.
   *
   * @param username the logged-in username
   * @param password the plain-text password (used for server re-authentication)
   * @param role     the user's role
   */
  public void setSession(String username, String password, String role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }

  /**
   * Returns the current session's username.
   *
   * @return the username, or null if not logged in
   */
  public String getUsername() {
    return username;
  }

  /**
   * Returns the current session's password.
   *
   * @return the password, or null if not logged in
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns the current session's role.
   *
   * @return the role, or null if not logged in
   */
  public String getRole() {
    return role;
  }

  /**
   * Clears all session data and destroys the singleton instance.
   */
  public void clear() {
    username = null;
    password = null;
    role = null;
    instance = null;
  }
}
