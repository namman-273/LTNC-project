package com.auction.service;

import com.auction.audit.AuditEvent;
import com.auction.audit.AuditEventType;
import com.auction.audit.AuditLogger;
import com.auction.exception.AuthenticationException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.util.DataManager;
import com.auction.util.SecurityUtils;
import java.util.HashMap;
import java.util.Map;

/**
   * .
   */
public class UserManager {

  // Singleton instance
  private static UserManager instance;

  // Yêu cầu: lưu Map<String, User>
  private Map<String, User> users = new HashMap<>();

  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String DEFAULT_ADMIN_PASS = "admin123";
  private static final String ROLE_ADMIN = "ADMIN";

  private UserManager() {
  }

  /**
   * get instance.
   */
  public static UserManager getInstance() {
    if (instance == null) {
      instance = new UserManager();
    }
    return instance;
  }

  /**
   * Lấy toàn bộ Map users để DataManager có thể lưu xuống file.
   */
  public Map<String, User> getUsers() {
    return users;
  }

  /**
   * Cập nhật lại Map users sau khi DataManager load từ file lên.
   */
  public void setUsers(Map<String, User> users) {
    if (users != null) {
      this.users = users;
    }
  }

  /**
   * Hỗ trợ đăng ký người dùng mới.
   */
  public boolean register(String username, String password, String role) {
    if (users.containsKey(username)) {
      // LOG FAILED REGISTRATION
      AuditLogger.getInstance().log(
          new AuditEvent.Builder()
              .eventType(AuditEventType.REGISTER_FAILURE)
              .username(username)
              .action("Đăng ký thất bại - username đã tồn tại")
              .result("FAILURE")
              .addMetadata("reason", "Username already exists")
              .addMetadata("attemptedRole", role)
              .build()
      );
      return false;
    }
    String hashedPassword = SecurityUtils.hashPassword(password, username);
    User newUser;
    // Phân quyền tạo đúng Object tương ứng
    switch (role.toUpperCase()) {
      case "ADMIN":
        newUser = new Admin(username, hashedPassword);
        break;
      case "SELLER":
        newUser = new Seller(username, hashedPassword);
        break;
      default:
        newUser = new Bidder(username, hashedPassword);
        break;
    }

    users.put(username, newUser);
    
    // LOG SUCCESS REGISTRATION
    AuditLogger.getInstance().log(
        new AuditEvent.Builder()
            .eventType(AuditEventType.REGISTER_SUCCESS)
            .username(username)
            .action("Đăng ký tài khoản thành công")
            .result("SUCCESS")
            .addMetadata("role", role.toUpperCase())
            .build()
    );
    
    // lưu file sau khi register thành công
    DataManager.getInstance().saveData();
    return true;
  }

  /**
   * Kiểm tra đăng nhập thực sựs.
   */
  public User login(String username, String password) throws AuthenticationException {
    User user = users.get(username);
    if (user == null) {
      // LOG FAILED LOGIN - User not found
      AuditLogger.getInstance().log(
          new AuditEvent.Builder()
              .eventType(AuditEventType.LOGIN_FAILURE)
              .username(username)
              .action("Đăng nhập thất bại - tài khoản không tồn tại")
              .result("FAILURE")
              .addMetadata("reason", "User not found")
              .build()
      );
      throw new AuthenticationException("Người dùng không tồn tại");
    }

    // PHẢI dùng username của user đó làm Salt để băm lại mật khẩu nhập vào
    String hashedInput = SecurityUtils.hashPassword(password, username);

    if (!user.getPassword().equals(hashedInput)) {
      // LOG FAILED LOGIN - Wrong password
      AuditLogger.getInstance().log(
          new AuditEvent.Builder()
              .eventType(AuditEventType.LOGIN_FAILURE)
              .username(username)
              .action("Đăng nhập thất bại - sai mật khẩu")
              .result("FAILURE")
              .addMetadata("reason", "Invalid password")
              .build()
      );
      throw new AuthenticationException("Sai mật khẩu");
    }

    // LOG SUCCESS LOGIN
    AuditLogger.getInstance().log(
        new AuditEvent.Builder()
            .eventType(AuditEventType.LOGIN_SUCCESS)
            .username(username)
            .action("Đăng nhập thành công")
            .result("SUCCESS")
            .addMetadata("role", user.getRole())
            .build()
    );

    return user;
  }

  // Tìm user theo username
  public User findUserByUsername(String username) {
    return users.get(username);
  }

  /**
   * Khởi tạo admin mặc định nếu dữ liệu trống.
   */
  public void initDefaultData() {
    if (users.isEmpty()) {
      register(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS, ROLE_ADMIN);
      System.out.println("Hệ thống trống. Đã tạo tài khoản admin mặc định.");
    }
  }
}
