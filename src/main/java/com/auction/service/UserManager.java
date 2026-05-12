package com.auction.service;

import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;
import com.auction.util.core.DataManager;
import com.auction.util.exception.AuthenticationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * .
 */
public class UserManager {

  // Singleton instance
  private static UserManager instance;

  // Yêu cầu: lưu Map<String, User>
  private Map<String, User> users = new ConcurrentHashMap<>();
  // Key là Email (đã viết thường), Value là Username
  private Map<String, String> emailToIndex = new ConcurrentHashMap<>();

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
  public void setUsers(Map<String, User> loadedUsers) {
    if (loadedUsers != null) {
      this.users = new ConcurrentHashMap<>(loadedUsers);
      // Quan trọng: Dựng lại Index ngay khi load dữ liệu từ file
      rebuildEmailIndex();
    }
  }

  private void rebuildEmailIndex() {
    emailToIndex.clear();
    for (User u : users.values()) {
      if (u.getEmail() != null) {
        emailToIndex.put(u.getEmail().toLowerCase(), u.getUsername());
      }
    }
  }

  /**
   * Hỗ trợ đăng ký người dùng mới.
   */
  public boolean register(String username, String password, String role, String email) {
    if (users.containsKey(username) || isEmailExists(email)) {
      return false;
    }

    User newUser;
    // Phân quyền tạo đúng Object tương ứng
    switch (role.toUpperCase()) {
      case "ADMIN":
        newUser = new Admin(username, password, email);
        break;
      case "SELLER":
        newUser = new Seller(username, password, email);
        break;
      default:
        newUser = new Bidder(username, password, email);
        break;
    }

    users.put(username, newUser);
    if (email != null)
      emailToIndex.put(email.toLowerCase(), username);
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

    if (!user.checkPassword(password)) {
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
    // Kiểm tra trực tiếp trong Map phụ (dùng toLowerCase để không phân biệt hoa
    // thường)
    return emailToIndex.containsKey(email.toLowerCase());
  }

  public boolean updateEmail(String username, String newEmail) {
    if (isEmailExists(newEmail)) {
      return false;
    }
    User user = users.get(username);
    if (user.getEmail() != null)
      emailToIndex.remove(user.getEmail().toLowerCase());

    // Cập nhật email và index mới
    user.setEmail(newEmail);
    emailToIndex.put(newEmail.toLowerCase(), username);

    DataManager.getInstance().saveData(); // Lưu xuống file .dat ngay
    return true;

  }

  // 3 Cập nhật Password
  public boolean updatePassword(String username, String oldPass, String newPass) {
    User user = users.get(username);
    if (!user.checkPassword(oldPass) || oldPass.equals(newPass)) {
      return false;
    }
    user.setPassword(newPass);
    DataManager.getInstance().saveData();
    return true;

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
