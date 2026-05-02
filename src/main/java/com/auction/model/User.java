package com.auction.model;

import com.auction.util.SecurityUtils;

public abstract class User extends Entity implements Observer {
    protected String username;
    private String password; // Lưu trữ dưới dạng hash đơn giản
    private String role; // "ADMIN", "SELLER", hoặc "BIDDER"
    private static final long serialVersionUID = 1L;
    private double balance;

    public User(String username, String password, String role ) {
        super(username);
        this.username = username;
        this.password = password;
        this.role = role;
        // Kiểm tra chặn số âm/NaN khi khởi tạo
        this.balance = 0.0;
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
        return this.password.equals(SecurityUtils.hashPassword(inputPassword, username));
    }

    public void update(String message) {
        // Sau này chỗ này sẽ hiển thị lên màn hình JavaFX
        System.out.println("[NOTIFY - " + username + "]: " + message);
    }

    public String toString() {
        return "User{" + "username='" + username + '\'' + ", role='" + role + '\'' + '}';
    }

    // --- CÁC PHƯƠNG THỨC GIAO DỊCH (THREAD-SAFE) ---

    /**
     * Lấy số dư hiện tại
     */
    public synchronized double getBalance() {
        return balance;
    }

    /**
     * Nạp tiền, nhận hoàn tiền (Refund) hoặc nhận tiền bán hàng
     */
    public synchronized void addBalance(double amount) {
        // Chốt chặn lỗi số 
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return;
        }
        this.balance += amount;

        // Thông báo biến động số dư qua hàm update (để Client nhận được)
        this.update("BALANCE_CHANGED|+" + amount + "|" + this.balance);
    }

    /**
     * Trừ tiền khi đặt Bid thành công
     * 
     * @return true nếu trừ tiền thành công, false nếu không đủ số dư
     */
    public synchronized boolean deductBalance(double amount) {
        // Chốt chặn lỗi số ma
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return false;
        }

        if (this.balance >= amount) {
            this.balance -= amount;
            this.update("BALANCE_CHANGED|-" + amount + "|" + this.balance);
            return true;
        }
        return false;
    }
}
