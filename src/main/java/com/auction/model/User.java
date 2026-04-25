package com.auction.model;

public abstract class User extends Entity implements Observer {
    protected String username;
    private String password; // Lưu trữ dưới dạng hash đơn giản
    private String role; // "ADMIN", "SELLER", hoặc "BIDDER"
    private static final long serialVersionUID = 1L;

    public User(String username, String password, String role) {
        super(username);
        this.username = username;
        this.password = hashPassword(password);  // ← thêm hash
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    private String hashPassword(String password) {
        if (password == null)
            return "";
        return String.valueOf(password.hashCode());
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public boolean checkPassword(String inputPassword) {
        return this.password.equals(hashPassword(inputPassword));
    }

    public void update(String message) {
        // Sau này chỗ này sẽ hiển thị lên màn hình JavaFX
        System.out.println("[NOTIFY - " + username + "]: " + message);
    }

    public String toString() {
        return "User{" + "username='" + username + '\'' + ", role='" + role + '\'' + '}';
    }
}
