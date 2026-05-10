package com.auction.model.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * Lớp đăng ký và quản lý các Factory tạo sản phẩm.
 * Áp dụng: Registry Pattern & Open/Closed Principle (OCP).
 */
public class ItemFactoryRegistry {

  // 1. Kho chứa các Factory (Registry)
  private static final Map<String, ItemFactory> registry = new HashMap<>();

  // 2. Nạp sẵn các loại mặt hàng hiện có khi hệ thống khởi động
  static {
    // Lưu ý: Đăng ký Key bằng chữ IN HOA để dễ dàng tìm kiếm không phân biệt
    // hoa/thường
    registry.put("ART", new ArtFactory());
    registry.put("ELECTRONICS", new ElectronicsFactory());
    registry.put("VEHICLE", new VehicleFactory());
  }

  /**
   * Lấy ra Factory tương ứng với loại Item.
   * TÊN HÀM GIỮ NGUYÊN để không vỡ code cũ.
   */
  public static ItemFactory getFactory(String type) {
    if (type == null) {
      throw new IllegalArgumentException("Loại sản phẩm không được để trống");
    }

    // Chuyển input về IN HOA (tương đương với ignoreCase cũ) để tìm trong Map
    ItemFactory factory = registry.get(type.toUpperCase());

    if (factory == null) {
      throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ: " + type);
    }

    return factory;
  }

  /**
   * MỞ RỘNG (OCP): Cho phép đăng ký thêm Factory mới từ bên ngoài
   * mà không cần phải mở file này ra sửa (Không cần thêm if/else).
   */
  public static void registerFactory(String type, ItemFactory factory) {
    if (type != null && factory != null) {
      registry.put(type.toUpperCase(), factory);
    }
  }
}
