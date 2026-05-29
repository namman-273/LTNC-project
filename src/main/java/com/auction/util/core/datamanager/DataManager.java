package com.auction.util.core.datamanager;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.User;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DataManager refactored theo SOLID principles.
 * SOLID Compliance:
 * - SRP: Delegate công việc cho các helper classes chuyên biệt
 * - OCP: Có thể extend qua interface DataPersistence
 * - LSP: N/A (không có inheritance)
 * - ISP: Interfaces nhỏ gọn, focused
 * - DIP: Phụ thuộc vào abstractions (DataPersistence interface).
 *
 */
public final class DataManager {
  
  private static final long AUTO_SAVE_INTERVAL_MS = 5000; // 5 giây
  
  // Helper components - mỗi component một trách nhiệm (SRP)
  private final DirtyFlagTracker dirtyTracker;
  private final AutoSaveScheduler autoSaveScheduler;
  private final DataSaver dataSaver;
  private final DataLoader dataLoader;
  
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  /**
   * Private constructor - khởi tạo tất cả components.
   */
  private DataManager() {
    // Khởi tạo persistence implementations (DIP - có thể thay đổi)
    DataPersistence<String, User> userPersistence = new FileDataPersistence<>();
    DataPersistence<String, Auction> auctionPersistence = new FileDataPersistence<>();
    DataPersistence<String, List<BidHistoryEntry>> historyPersistence = new FileDataPersistence<>();
    
    // Khởi tạo components
    this.dirtyTracker = new DirtyFlagTracker();
    this.dataSaver = new DataSaver(userPersistence, auctionPersistence, 
        historyPersistence, dirtyTracker);
    this.dataLoader = new DataLoader(userPersistence, auctionPersistence, 
        historyPersistence);
    this.autoSaveScheduler = new AutoSaveScheduler(AUTO_SAVE_INTERVAL_MS);
    
    // Bắt đầu auto-save
    startAutoSave();
    
    // Đăng ký shutdown hook
    registerShutdownHook();
  }

  /**
   * Singleton Pattern.
   */
  private static class Holder {
    private static final DataManager INSTANCE = new DataManager();
  }

  public static DataManager getInstance() {
    return Holder.INSTANCE;
  }

  /**
   * Bắt đầu auto-save.
   */
  private void startAutoSave() {
    autoSaveScheduler.start(() -> dataSaver.saveAllDirty());
  }

  // ========== PUBLIC API - BACKWARD COMPATIBLE ==========

  /**
   * Đánh dấu users cần save.
   * API mới - recommended.
   */
  public void markUsersDirty() {
    dirtyTracker.markUsersDirty();
  }

  /**
   * Đánh dấu auctions cần save.
   * API mới - recommended.
   */
  public void markAuctionsDirty() {
    dirtyTracker.markAuctionsDirty();
  }

  /**
   * Đánh dấu history cần save.
   * API mới - recommended.
   */
  public void markHistoryDirty() {
    dirtyTracker.markHistoryDirty();
  }

  /**
   * Save ngay lập tức (dùng cho critical operations).
   * API cũ - vẫn hoạt động như bình thường.
   */
  
  public synchronized void saveData() {
    if (isShutdown.get()) {
      System.out.println("[DataManager] Already shutdown, skipping save");
      return;
    }

    System.out.println("[DataManager] Saving data immediately...");
    dataSaver.saveAll();
    System.out.println("[DataManager] Save complete");
  }

  /**
   * Load toàn bộ dữ liệu khi khởi động.
   * API cũ - vẫn hoạt động như bình thường.
   */
  
  public synchronized void loadData() {
    dataLoader.loadAll();
  }

  // ========== SHUTDOWN MANAGEMENT ==========

  /**
   * Shutdown gracefully.
   */
  private void shutdown() {
    if (isShutdown.compareAndSet(false, true)) {
      System.out.println("[DataManager] Shutting down...");

      // Dừng auto-save scheduler
      autoSaveScheduler.shutdown(2);

      // Save cuối cùng
      System.out.println("[DataManager] Performing final save...");
      dirtyTracker.markAllDirty();
      dataSaver.saveAll();

      System.out.println("[DataManager] Shutdown complete");
    }
  }

  /**
   * Đăng ký shutdown hook.
   */
  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (!isShutdown.get()) {
        System.out.println("[DataManager] JVM shutting down, performing final save...");
        shutdown();
      }
    }, "DataManager-ShutdownHook"));
  }

  // ==========  MONITORING ==========



  /**
   * Kiểm tra có data nào cần save không.
   */
  public boolean hasAnyDirty() {
    return dirtyTracker.hasAnyDirty();
  }

  /**
   * Lấy auto-save interval.
   */
  public long getAutoSaveIntervalMs() {
    return autoSaveScheduler.getIntervalMs();
  }
}