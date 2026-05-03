package com.auction.model;

import com.auction.util.SecurityUtils;

public abstract class User extends Entity implements Observer {
    protected String username;
    private String password; // Lưu trữ dưới dạng hash đơn giản
    private String role; // "ADMIN", "SELLER", hoặc "BIDDER"
    private static final long serialVersionUID = 1L;
    private double balance;

    // LOCK RIÊNG: Đảm bảo mọi giao dịch nạp/rút không bị xen kẽ (Atomic Swap)
    private transient Object balanceLock = new Object();

    public User(String username, String password, String role) {
        super(username);
        this.username = username;
        this.password = password;
        this.role = role;
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

    private Object getLock() {
        if (balanceLock == null) {
            balanceLock = new Object(); // Khởi tạo lại nếu vừa load từ file lên
        }
        return balanceLock;
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
    public void addBalance(double amount) {
        synchronized (getLock()) {
            // Chốt chặn lỗi số
            if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
                return;
            }
            // Chúng ta đặt giới hạn an toàn là Double.MAX_VALUE / 2
            if (this.balance + amount > Double.MAX_VALUE / 2) {
                // Nếu vượt quá, ta chặn lại ở mức trần an toàn thay vì để nó thành Infinity
                this.balance = Double.MAX_VALUE / 2;
                System.out.println("[WARNING] Tài khoản " + username + " đã đạt hạn mức tối đa.");
            } else {
                // Nếu an toàn thì mới cộng dồn như bình thường
                this.balance += amount;
            }

            // Thông báo biến động số dư qua hàm update (để Client nhận được)
            this.update("BALANCE_CHANGED|+" + (long) amount + "|" + (long) this.balance);
        }
    }

    /**
     * Trừ tiền khi đặt Bid thành công
     * 
     * @return true nếu trừ tiền thành công, false nếu không đủ số dư
     */
    public boolean deductBalance(double amount) {
        synchronized (getLock()) {
            // Chốt chặn lỗi số ma
            if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
                return false;
            }

            if (this.balance >= amount) {
                this.balance -= amount;
                this.update("BALANCE_CHANGED|-" + (long) amount + "|" + (long) this.balance);
                return true;
            }
            return false;
        }
    }
}
