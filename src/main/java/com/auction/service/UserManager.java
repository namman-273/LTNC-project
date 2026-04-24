package com.auction.service;

import com.auction.model.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class UserManager implements Serializable {
    private static final long serialVersionUID = 1L;

    // Singleton instance
    private static UserManager instance;

    // Yêu cầu: lưu Map<String, User>
    private Map<String, User> users = new HashMap<>();

    private UserManager() {
        // Tạo sẵn Admin mặc định để hệ thống luôn có người quản trị
        register("admin", "admin123", "ADMIN");
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Fix lỗi Singleton bị phá vỡ khi Deserialize
     */
    protected Object readResolve() {
        return getInstance();
    }

    /**
     * Hỗ trợ đăng ký người dùng mới
     */
    public boolean register(String username, String password, String role) {
        if (users.containsKey(username))
            return false;

        User newUser;
        // Phân quyền tạo đúng Object tương ứng
        switch (role.toUpperCase()) {
            case "ADMIN":
                newUser = new Admin(username, password);
                break;
            case "SELLER":
                newUser = new Seller(username, password);
                break;
            default:
                newUser = new Bidder(username, password);
                break;
        }

        users.put(username, newUser);
        return true;
    }

    /**
     * Kiểm tra đăng nhập thực sựs
     */
    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    // Tìm user theo ID (username)
    public User findUserById(String id) {
        return users.get(id);
    }

    // Cập nhật instance khi load từ DataManager
    public static void setInstance(UserManager manager) {
        instance = manager;
    }
}
