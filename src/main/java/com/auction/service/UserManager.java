package com.auction.service;

import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;
import com.auction.util.core.DataManager;
import com.auction.util.core.SecurityUtils;
import com.auction.util.exception.AuthenticationException;
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
  private static final String ADMIN_EMAIl = "admin123321@gmail.com";

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
  public boolean register(String username, String password, String role, String email) {
    if (users.containsKey(username)) {
      return false;
    }
    if (isEmailExists(email)) {
      return false;
    }
    String hashedPassword = SecurityUtils.hashPassword(password, username);
    User newUser;
    // Phân quyền tạo đúng Object tương ứng
    switch (role.toUpperCase()) {
      case "ADMIN":
        newUser = new Admin(username, hashedPassword, email);
        break;
      case "SELLER":
        newUser = new Seller(username, hashedPassword, email);
        break;
      default:
        newUser = new Bidder(username, hashedPassword, email);
        break;
    }

    users.put(username, newUser);

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
      throw new AuthenticationException("Người dùng không tồn tại");
    }

    // PHẢI dùng username của user đó làm Salt để băm lại mật khẩu nhập vào
    String hashedInput = SecurityUtils.hashPassword(password, username);

    if (!user.getPassword().equals(hashedInput)) {
      throw new AuthenticationException("Sai mật khẩu");
    }

    return user;
  }

  /**
   * Kiểm tra xem email đã được sử dụng bởi Seller hoặc Bidder nào khác chưa.
   * 
   * @param email Email cần kiểm tra'.
   * @return true nếu đã tồn tại, false nếu chưa
   */
  private boolean isEmailExists(String email) {
    if (email == null || email.isEmpty())
      return false;

    // Duyệt qua toàn bộ danh sách User hiện có
    for (User user : users.values()) {
      // Chỉ so sánh nếu user đó có email (Seller/Bidder)
      if (email.equalsIgnoreCase(user.getEmail())) {
        return true;
      }
    }
    return false;
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
      register(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS, ROLE_ADMIN, ADMIN_EMAIl);
      System.out.println("Hệ thống trống. Đã tạo tài khoản admin mặc định.");
    }
  }
}
