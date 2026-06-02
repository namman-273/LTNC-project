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
   */
  void save(Map<K, V> data, String identifier) throws DataPersistenceException;
  
  /**
   * Load data từ storage.
   * 
   */
  Map<K, V> load(String identifier) throws DataPersistenceException;
  
  /**
   * Exception cho data persistence operations.
   */
  class DataPersistenceException extends Exception {
    
    public DataPersistenceException(String message) {
      super(message);
    }
    
    public DataPersistenceException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}