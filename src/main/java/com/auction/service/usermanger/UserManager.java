package com.auction.service.usermanger;

import com.auction.model.entities.user.User;
import com.auction.util.exception.AuthenticationException;
import java.util.Map;

/**
 * UserManager sử dụng Facade Pattern.
 * Delegate các công việc cho các helper classes chuyên biệt.
 * Tuân thủ SOLID principles.
 */
public class UserManager {

  // Singleton instance
  private static UserManager instance;

  // Helper classes - mỗi class có một trách nhiệm riêng (SRP)
  private final UserRepository userRepository;
  private final EmailIndexer emailIndexer;
  private final UserFactory userFactory;
  private final AuthenticationService authenticationService;
  private final UserValidator userValidator;
  private final DataPersistenceService persistenceService;

  // Constants
  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String DEFAULT_ADMIN_PASS = "admin123";
  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ADMIN_EMAIL = "admin123321@gmail.com";

  private UserManager() {
    // Khởi tạo các helper classes
    this.userRepository = new UserRepository();
    this.emailIndexer = new EmailIndexer();
    this.userFactory = new UserFactory();
    this.authenticationService = new AuthenticationService(userRepository);
    this.userValidator = new UserValidator(userRepository, emailIndexer);
    this.persistenceService = new DataPersistenceService();
  }

  /**
   * Get singleton instance.
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
    return userRepository.getAll();
  }

  /**
   * Cập nhật lại Map users sau khi DataManager load từ file lên.
   */
  public void setUsers(Map<String, User> loadedUsers) {
    if (loadedUsers != null) {
      userRepository.setAll(loadedUsers);
      // Quan trọng: Dựng lại Index ngay khi load dữ liệu từ file
      emailIndexer.rebuildIndex(userRepository.getAll());
    }
  }

  /**
   * Hỗ trợ đăng ký người dùng mới.
   */
  public boolean register(String username, String password, String role, String email) {
    // Validate trước khi đăng ký
    if (!userValidator.canRegister(username, email)) {
      return false;
    }

    // Tạo user object theo role (Factory Pattern)
    User newUser = userFactory.createUser(username, password, role, email);

    // Lưu user vào repository
    userRepository.add(username, newUser);

    // Cập nhật email index
    if (email != null) {
      emailIndexer.addEmail(email, username);
    }

    // Lưu file sau khi register thành công
    persistenceService.saveData();
    
    return true;
  }

  /**
   * Kiểm tra đăng nhập thực sự.
   */
  public User login(String username, String password) throws AuthenticationException {
    return authenticationService.authenticate(username, password);
  }

  /**
   * Cập nhật email của user.
   */
  public boolean updateEmail(String username, String newEmail) {
    // Validate email mới
    if (!userValidator.canUpdateEmail(newEmail)) {
      return false;
    }

    User user = userRepository.findByUsername(username);
    if (user == null) {
      return false;
    }

    // Xóa email cũ khỏi index
    if (user.getEmail() != null) {
      emailIndexer.removeEmail(user.getEmail());
    }

    // Cập nhật email và index mới
    user.setEmail(newEmail);
    emailIndexer.addEmail(newEmail, username);

    // Lưu xuống file .dat ngay
    persistenceService.saveData();
    
    return true;
  }

  /**
   * Cập nhật Password.
   */
  public boolean updatePassword(String username, String oldPass, String newPass) {
    User user = userRepository.findByUsername(username);
    if (user == null) {
      return false;
    }

    // Validate password change
    if (!authenticationService.validatePasswordChange(user, oldPass, newPass)) {
      return false;
    }

    // Cập nhật password
    user.setPassword(newPass);
    
    // Lưu xuống file
    persistenceService.saveData();
    
    return true;
  }

  /**
   * Tìm user theo username.
   */
  public User findUserByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  /**
   * Khởi tạo admin mặc định nếu dữ liệu trống.
   */
  public void initDefaultData() {
    if (userRepository.isEmpty()) {
      register(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS, ROLE_ADMIN, ADMIN_EMAIL);
      System.out.println("Hệ thống trống. Đã tạo tài khoản admin mặc định.");
    }
  }
}