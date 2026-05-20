package com.auction.service.usermanger;

import com.auction.util.core.DataManager;

/**
 * Service để xử lý persistence operations cho UserManager.
 * Tuân thủ Dependency Inversion Principle - có thể thay đổi implementation dễ
 * dàng.
 */
public class DataPersistenceService {

  /**
   * Đánh dấu users data cần save (sẽ auto-save sau 5 giây).
   */
  public void saveData() {
    DataManager.getInstance().markUsersDirty();
  }

  /**
   * Save ngay lập tức (dùng cho critical operations).
   */
  public void saveDataImmediately() {
    DataManager.getInstance().saveData();
  }
}