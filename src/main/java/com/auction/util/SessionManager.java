package com.auction.util;

public class SessionManager {

    private static SessionManager instance;
    private String username;
    private String password; // Chỉ dùng nội bộ, không expose ra ngoài
    private String role;

    private SessionManager() {}

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

    public String getUsername() { return username; }
    public String getRole() { return role; }

    // Package-private - chỉ dùng nội bộ trong package util
    public String getPasswordInternal() { return password; }
    public String getPassword() { return password; }

    public void clear() {
        username = null;
        password = null;
        role = null;
        instance = null;
    }
}