package com.auction.util.core;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionManager {

  private String username;
  private String password;
  private String role;

  // Sử dụng cặp khóa Đọc - Ghi tách biệt
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
  private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

  private SessionManager() {
  }

  private static class SingletonHolder {
    private static final SessionManager INSTANCE = new SessionManager();
  }

  public static SessionManager getInstance() {
    return SingletonHolder.INSTANCE;
  }

  /**
   * Khóa GHI (WriteLock): Khi đang ghi, cấm tất cả các luồng khác Đọc hoặc Ghi.
   */
  public void setSession(String username, String password, String role) {
    writeLock.lock();
    try {
      this.username = username;
      this.password = password;
      this.role = role;
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Khóa ĐỌC (ReadLock): Hàng trăm luồng có thể vào đọc getUsername(), getRole()
   * cùng một lúc mà không bị nghẽn, miễn là không có ai đang ghi.
   */
  public String getUsername() {
    readLock.lock();
    try {
      return username;
    } finally {
      readLock.unlock();
    }
  }

  public String getPassword() {
    readLock.lock();
    try {
      return password;
    } finally {
      readLock.unlock();
    }
  }

  public String getRole() {
    readLock.lock();
    try {
      return role;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Khóa GHI (WriteLock).
   */
  public void clear() {
    writeLock.lock();
    try {
      this.username = null;
      this.password = null;
      this.role = null;
    } finally {
      writeLock.unlock();
    }
  }
}