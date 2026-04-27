package com.auction.service;

import com.auction.model.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.auction.exception.AuthenticationException;
import com.auction.util.DataManager;
import com.auction.util.SecurityUtils;;

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
        //lưu file sau khi register thành công
        DataManager.getInstance().saveData();
        return true;
    }

    /**
     * Kiểm tra đăng nhập thực sựs
     */
    public User login(String username, String password) throws AuthenticationException {
        User user = users.get(username);
        if (user == null) {
            throw new AuthenticationException("Người dùng không tồn tại");
        }

        // PHẢI dùng username của user đó làm Salt để băm lại mật khẩu nhập vào
        String hashedInput = SecurityUtils.hashPassword(password,username);

        if (!user.getPassword().equals(hashedInput)) {
            throw new AuthenticationException("Sai mật khẩu");
        }

        return user;
    }

    // Tìm user theo username
    public User findUserByUsername(String username) {
        return users.get(username);
    }

    // Cập nhật instance khi load từ DataManager
    public static void setInstance(UserManager manager) {
        instance = manager;
    }
}
