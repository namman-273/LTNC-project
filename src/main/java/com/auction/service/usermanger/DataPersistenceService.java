package com.auction.service.usermanger;

import com.auction.util.core.DataManager;

/**
 * Service để xử lý persistence operations.
 * Tuân thủ Dependency Inversion Principle - có thể thay đổi implementation dễ
 * dàng.
 */
public class DataPersistenceService {

  /**
   * Lưu dữ liệu xuống storage.
   */
  public void saveData() {
    DataManager.getInstance().saveData();
  }
}
