package com.auction.util.core;

/**
 * Hệ thống Quản lý Phiên làm việc (Session) phía Client.
 * Áp dụng Bill Pugh Singleton Thread-safe tối ưu hiệu năng và giữ lại Password.
 */
public class SessionManager {

  private String username;
  private String password;
  private String role;

  // 1. Private constructor ngăn chặn tạo đối tượng từ bên ngoài
  private SessionManager() {
  }

  // 2. Bill Pugh Holder: Tự động Thread-safe bởi ClassLoader của JVM, tối ưu hơn
  // synchronized cũ
  private static class SingletonHolder {
    private static final SessionManager INSTANCE = new SessionManager();
  }

  public static SessionManager getInstance() {
    return SingletonHolder.INSTANCE;
  }

  /**
   * Khởi tạo session khi đăng nhập thành công.
   * Đồng bộ hóa (synchronized) để đảm bảo an toàn khi luồng Socket ghi đè dữ
   * liệu.
   */
  public synchronized void setSession(String username, String password, String role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }

  public synchronized String getUsername() {
    return username;
  }

  public synchronized String getPassword() {
    return password;
  }

  public synchronized String getRole() {
    return role;
  }


  /**
   * Đăng xuất: Xóa sạch ruột dữ liệu để bảo mật, giữ nguyên xác Object Singleton.
   */
  public synchronized void clear() {
    this.username = null;
    this.password = null;
    this.role = null;
  }
}