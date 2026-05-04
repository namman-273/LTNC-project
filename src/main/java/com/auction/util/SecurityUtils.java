package com.auction.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

// Hàm hỗ trợ băm mật khẩu chuẩn SHA-256
public class SecurityUtils {
  public static String hashPassword(String password, String salt) {
    if (password == null || salt == null) {
      return null;
    }
    try {
      // Sử dụng thuật toán SHA-256 (Tiêu chuẩn phổ biến)
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      // Trộn Salt (username) vào mật khẩu
      String saltedPassword = password + salt;
      // Băm mật khẩu (kết quả là mảng byte)
      byte[] hash = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

      // Chuyển mảng byte sang chuỗi Base64 để lưu vào file .dat cho dễ
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      // Trong trường hợp hy hữu thuật toán không tồn tại
      throw new RuntimeException("Lỗi hệ thống khi băm mật khẩu", e);
    }
  }
}
