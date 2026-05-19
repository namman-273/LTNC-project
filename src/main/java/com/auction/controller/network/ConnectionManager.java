package com.auction.controller.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton quản lý tất cả ClientHandler đang kết nối.
 * Cho phép gửi thông báo trực tiếp đến user theo username.
 */
public class ConnectionManager {
  private static volatile ConnectionManager instance;

  // Map username -> ClientHandler
  private final Map<String, ClientHandler> activeConnections = new ConcurrentHashMap<>();

  private ConnectionManager() {
  }

  /**
   * Lấy instance singleton.
   */
  public static ConnectionManager getInstance() {
    if (instance == null) {
      synchronized (ConnectionManager.class) {
        if (instance == null) {
          instance = new ConnectionManager();
        }
      }
    }
    return instance;
  }

  /**
   * Đăng ký một ClientHandler khi user login.
   */
  public void registerConnection(String username, ClientHandler handler) {
    if (username != null && handler != null) {
      activeConnections.put(username, handler);
      System.out.println("[CONNECTION] Đã đăng ký kết nối cho user: " + username);
    }
  }

  /**
   * Hủy đăng ký ClientHandler khi user disconnect.
   */
  public void unregisterConnection(String username) {
    if (username != null) {
      activeConnections.remove(username);
      System.out.println("[CONNECTION] Đã hủy kết nối cho user: " + username);
    }
  }

  /**
   * Gửi thông báo trực tiếp đến user theo username.
   * Không cần qua observer pattern.
   * 
   * @return true nếu gửi thành công, false nếu user không online
   */
  public boolean sendDirectMessage(String username, String message) {
    ClientHandler handler = activeConnections.get(username);
    if (handler != null) {
      handler.sendMessage(message);
      return true;
    }
    return false;
  }

}