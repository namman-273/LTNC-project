package com.auction.service.usermanger;

import com.auction.model.entities.user.User;
import com.auction.util.exception.AuthenticationException;

/**
 * Service xử lý authentication logic.
 * Tuân thủ Single Responsibility Principle.
 */
public class AuthenticationService {

  private final UserRepository userRepository;

  public AuthenticationService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Xác thực đăng nhập.
   */
  public User authenticate(String username, String password) throws AuthenticationException {
    User user = userRepository.findByUsername(username);
    
    if (user == null) {
      throw new AuthenticationException("Người dùng không tồn tại");
    }

    if (!user.checkPassword(password)) {
      throw new AuthenticationException("Sai mật khẩu");
    }

    return user;
  }

  /**
   * Kiểm tra mật khẩu cũ và validate mật khẩu mới.
   */
  public boolean validatePasswordChange(User user, String oldPass, String newPass) {
    if (user == null) {
      return false;
    }
    
    // Kiểm tra mật khẩu cũ đúng không
    if (!user.checkPassword(oldPass)) {
      return false;
    }
    
    // Mật khẩu mới không được trùng mật khẩu cũ
    if (oldPass.equals(newPass)) {
      return false;
    }
    
    return true;
  }
}