package com.auction.service.usermanger;

import com.auction.model.entities.user.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý email index để tra cứu nhanh user theo email.
 * Tuân thủ Single Responsibility Principle.
 */
public class EmailIndexer {

  // Key là Email (đã viết thường), Value là Username
  private Map<String, String> emailToIndex = new ConcurrentHashMap<>();

  /**
   * Kiểm tra xem email đã tồn tại chưa.
   */
  public boolean isEmailExists(String email) {
    if (email == null || email.isEmpty()) {
      return false;
    }
    return emailToIndex.containsKey(email.toLowerCase());
  }

  /**
   * Thêm email vào index.
   */
  public void addEmail(String email, String username) {
    if (email != null && !email.isEmpty()) {
      emailToIndex.put(email.toLowerCase(), username);
    }
  }

  /**
   * Xóa email khỏi index.
   */
  public void removeEmail(String email) {
    if (email != null && !email.isEmpty()) {
      emailToIndex.remove(email.toLowerCase());
    }
  }

  /**
   * Dựng lại toàn bộ index từ Map users.
   */
  public void rebuildIndex(Map<String, User> users) {
    emailToIndex.clear();
    for (User user : users.values()) {
      if (user.getEmail() != null) {
        emailToIndex.put(user.getEmail().toLowerCase(), user.getUsername());
      }
    }
  }

  /**
   * Xóa toàn bộ index.
   */
  public void clear() {
    emailToIndex.clear();
  }
}
