package com.auction.util.core;

/**
 * Interface định nghĩa các thao tác lưu trữ dữ liệu.
 * Giúp tách biệt logic nghiệp vụ khỏi cách lưu trữ vật lý (File, DB, v.v.).
 */
public interface IDataStorage {
  void loadData();

  
  void saveData();
}
