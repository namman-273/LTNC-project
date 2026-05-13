package com.auction.service.usermanger;

import com.auction.model.entities.user.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository pattern để quản lý storage của users.
 * Tuân thủ Single Responsibility Principle.
 */
public class UserRepository {

  private Map<String, User> users = new ConcurrentHashMap<>();

  /**
   * Lấy toàn bộ Map users.
   */
  public Map<String, User> getAll() {
    return users;
  }

  /**
   * Cập nhật lại Map users (từ file load).
   */
  public void setAll(Map<String, User> loadedUsers) {
    if (loadedUsers != null) {
      this.users = new ConcurrentHashMap<>(loadedUsers);
    }
  }

  /**
   * Thêm user mới.
   */
  public void add(String username, User user) {
    users.put(username, user);
  }

  /**
   * Tìm user theo username.
   */
  public User findByUsername(String username) {
    return users.get(username);
  }

  /**
   * Kiểm tra user có tồn tại không.
   */
  public boolean exists(String username) {
    return users.containsKey(username);
  }

  /**
   * Kiểm tra repository có rỗng không.
   */
  public boolean isEmpty() {
    return users.isEmpty();
  }
}
