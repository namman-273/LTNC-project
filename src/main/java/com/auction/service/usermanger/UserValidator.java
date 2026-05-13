package com.auction.service.usermanger;

/**
 * Validator để kiểm tra các điều kiện trước khi thực hiện operations.
 * Tuân thủ Single Responsibility Principle.
 */
public class UserValidator {

  private final UserRepository userRepository;
  private final EmailIndexer emailIndexer;

  public UserValidator(UserRepository userRepository, EmailIndexer emailIndexer) {
    this.userRepository = userRepository;
    this.emailIndexer = emailIndexer;
  }

  /**
   * Kiểm tra có thể đăng ký user mới không.
   */
  public boolean canRegister(String username, String email) {
    // Username đã tồn tại
    if (userRepository.exists(username)) {
      return false;
    }

    // Email đã được sử dụng
    if (emailIndexer.isEmailExists(email)) {
      return false;
    }

    return true;
  }

  /**
   * Kiểm tra có thể cập nhật email không.
   */
  public boolean canUpdateEmail(String newEmail) {
    return !emailIndexer.isEmailExists(newEmail);
  }
}
