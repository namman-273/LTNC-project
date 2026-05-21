package com.auction.util.core.datamanager;

import java.util.Map;

/**
 * Interface cho data persistence operations.
 * Tuân thủ Dependency Inversion Principle - phụ thuộc vào abstraction.
 * Tuân thủ Interface Segregation Principle - interface nhỏ gọn.
 */
public interface DataPersistence<K, V> {
  
  /**
   * Lưu data xuống storage.
   * 
   * @param data Map cần lưu
   * @param identifier Identifier cho loại data (vd: "users", "auctions")
   * @throws DataPersistenceException nếu lưu thất bại
   */
  void save(Map<K, V> data, String identifier) throws DataPersistenceException;
  
  /**
   * Load data từ storage.
   * 
   * @param identifier Identifier cho loại data
   * @return Map data đã load, hoặc null nếu không tồn tại
   * @throws DataPersistenceException nếu load thất bại
   */
  Map<K, V> load(String identifier) throws DataPersistenceException;
  
  /**
   * Exception cho data persistence operations.
   */
  class DataPersistenceException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public DataPersistenceException(String message) {
      super(message);
    }
    
    public DataPersistenceException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}