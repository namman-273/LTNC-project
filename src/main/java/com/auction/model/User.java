package com.auction.model;

import com.auction.util.SecurityUtils;

public abstract class User extends Entity implements Observer {
    protected String username;
    private String password; // Lưu trữ dưới dạng hash đơn giản
    private String role; // "ADMIN", "SELLER", hoặc "BIDDER"
    private static final long serialVersionUID = 1L;

    public User(String username, String password, String role) {
        super(username);
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

    public boolean checkPassword(String inputPassword) {
        return this.password.equals(SecurityUtils.hashPassword(inputPassword,username));
    }

    public void update(String message) {
        // Sau này chỗ này sẽ hiển thị lên màn hình JavaFX
        System.out.println("[NOTIFY - " + username + "]: " + message);
    }

    public String toString() {
        return "User{" + "username='" + username + '\'' + ", role='" + role + '\'' + '}';
    }
}
