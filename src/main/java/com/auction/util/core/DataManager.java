package com.auction.util.core;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.User;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.bidhistorymanager.BidHistoryManager;
import com.auction.service.usermanger.UserManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DataManager tối ưu với:
 * - Auto-save định kỳ thay vì save ngay lập tức
 * - Lock riêng biệt cho từng loại data (users, auctions, history)
 * - Copy-on-write để tránh lock lâu
 * - Dirty flag tracking để chỉ save khi có thay đổi
 * - Graceful shutdown
 */
public final class DataManager {
  private static final String AUCTION_DATA_FILE = "auctions.dat";
  private static final String USER_DATA_FILE = "users.dat";
  private static final String HISTORY_DATA_FILE = "history.dat";
  private static final String TEMP_EXT = ".tmp";

  // Auto-save interval (milliseconds)
  private static final long AUTO_SAVE_INTERVAL_MS = 5000; // 5 giây

  // Dirty flags - đánh dấu data nào cần save
  private final AtomicBoolean usersDirty = new AtomicBoolean(false);
  private final AtomicBoolean auctionsDirty = new AtomicBoolean(false);
  private final AtomicBoolean historyDirty = new AtomicBoolean(false);

  // Lock riêng cho từng loại data - cho phép save song song
  private final ReentrantReadWriteLock usersLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock auctionsLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock historyLock = new ReentrantReadWriteLock();

  // Scheduler cho auto-save
  private final ScheduledExecutorService autoSaveScheduler;

  // Flag để kiểm tra đã shutdown chưa
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  private DataManager() {
    // Khởi tạo auto-save scheduler
    this.autoSaveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "DataManager-AutoSave");
      t.setDaemon(true); // Daemon thread để không block JVM shutdown
      return t;
    });

    // Lên lịch auto-save định kỳ
    startAutoSave();

    // Đăng ký shutdown hook để save trước khi tắt
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
   * Bắt đầu auto-save định kỳ.
   */
  private void startAutoSave() {
    autoSaveScheduler.scheduleWithFixedDelay(() -> {
      try {
        autoSaveIfDirty();
      } catch (Exception e) {
        System.err.println("[DataManager] Lỗi auto-save: " + e.getMessage());
        e.printStackTrace();
      }
    }, AUTO_SAVE_INTERVAL_MS, AUTO_SAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);

    System.out.println("[DataManager] Auto-save đã được kích hoạt (mỗi "
        + (AUTO_SAVE_INTERVAL_MS / 1000) + "s)");
  }

  /**
   * Auto-save chỉ khi có thay đổi (dirty flag = true).
   */
  private void autoSaveIfDirty() {
    boolean saved = false;

    // Save users nếu dirty
    if (usersDirty.get()) {
      saveUsersData();
      saved = true;
    }

    // Save auctions nếu dirty
    if (auctionsDirty.get()) {
      saveAuctionsData();
      saved = true;
    }

    // Save history nếu dirty
    if (historyDirty.get()) {
      saveHistoryData();
      saved = true;
    }

    if (saved) {
      System.out.println("[DataManager] Auto-save hoàn tất");
    }
  }

  /**
   * Đánh dấu users cần save.
   * Gọi method này thay vì saveData() trực tiếp.
   */
  public void markUsersDirty() {
    usersDirty.set(true);
  }

  /**
   * Đánh dấu auctions cần save.
   */
  public void markAuctionsDirty() {
    auctionsDirty.set(true);
  }

  /**
   * Đánh dấu history cần save.
   */
  public void markHistoryDirty() {
    historyDirty.set(true);
  }

  /**
   * Save ngay lập tức (dùng cho critical operations hoặc shutdown).
   */

  public synchronized void saveData() {
    if (isShutdown.get()) {
      System.out.println("[DataManager] Đã shutdown, bỏ qua save request");
      return;
    }

    System.out.println("[DataManager] Đang save dữ liệu ngay lập tức...");

    saveUsersData();
    saveAuctionsData();
    saveHistoryData();

    System.out.println("[DataManager] Save hoàn tất");
  }

  /**
   * Save riêng users data (thread-safe).
   */
  private void saveUsersData() {
    if (!usersDirty.compareAndSet(true, false)) {
      return; // Không dirty, bỏ qua
    }

    usersLock.writeLock().lock();
    try {
      // Copy data để giảm thời gian lock
      Map<String, User> usersSnapshot = new HashMap<>(
          UserManager.getInstance().getUsers());

      // Release lock trước khi serialize (I/O chậm)
      usersLock.writeLock().unlock();

      // Serialize và save (không cần lock nữa)
      saveMapToFile(usersSnapshot, USER_DATA_FILE);

    } catch (Exception e) {
      System.err.println("[DataManager] Lỗi save users: " + e.getMessage());
      usersDirty.set(true); // Đánh dấu lại để retry
    } finally {
      // Đảm bảo unlock nếu exception xảy ra trước unlock
      if (usersLock.writeLock().isHeldByCurrentThread()) {
        usersLock.writeLock().unlock();
      }
    }
  }

  /**
   * Save riêng auctions data (thread-safe).
   */
  private void saveAuctionsData() {
    if (!auctionsDirty.compareAndSet(true, false)) {
      return; // Không dirty, bỏ qua
    }

    auctionsLock.writeLock().lock();
    try {
      // Copy data để giảm thời gian lock
      Map<String, Auction> auctionsSnapshot = new HashMap<>(
          AuctionService.getInstance().getAuctionsMap());

      // Release lock trước khi serialize
      auctionsLock.writeLock().unlock();

      // Serialize và save
      saveMapToFile(auctionsSnapshot, AUCTION_DATA_FILE);

    } catch (Exception e) {
      System.err.println("[DataManager] Lỗi save auctions: " + e.getMessage());
      auctionsDirty.set(true); // Đánh dấu lại để retry
    } finally {
      if (auctionsLock.writeLock().isHeldByCurrentThread()) {
        auctionsLock.writeLock().unlock();
      }
    }
  }

  /**
   * Save riêng history data (thread-safe).
   */
  private void saveHistoryData() {
    if (!historyDirty.compareAndSet(true, false)) {
      return; // Không dirty, bỏ qua
    }

    historyLock.writeLock().lock();
    try {
      // Copy data để giảm thời gian lock
      Map<String, List<BidHistoryEntry>> historySnapshot = new HashMap<>(
          BidHistoryManager.getInstance().getHistoryMap());

      // Release lock trước khi serialize
      historyLock.writeLock().unlock();

      // Serialize và save
      saveMapToFile(historySnapshot, HISTORY_DATA_FILE);

    } catch (Exception e) {
      System.err.println("[DataManager] Lỗi save history: " + e.getMessage());
      historyDirty.set(true); // Đánh dấu lại để retry
    } finally {
      if (historyLock.writeLock().isHeldByCurrentThread()) {
        historyLock.writeLock().unlock();
      }
    }
  }

  /**
   * Load toàn bộ dữ liệu khi khởi động (thread-safe).
   */

  public synchronized void loadData() {
    System.out.println("[DataManager] Đang load dữ liệu...");

    // Load users
    usersLock.writeLock().lock();
    try {
      Map<String, User> loadedUsers = loadMapFromFile(USER_DATA_FILE);
      if (loadedUsers != null) {
        UserManager.getInstance().setUsers(loadedUsers);
        System.out.println("[DataManager] Đã load " + loadedUsers.size() + " users");
      }
    } finally {
      usersLock.writeLock().unlock();
    }

    // Load auctions
    auctionsLock.writeLock().lock();
    try {
      Map<String, Auction> loadedAuctions = loadMapFromFile(AUCTION_DATA_FILE);
      if (loadedAuctions != null) {
        AuctionService.getInstance().setAuctions(loadedAuctions);

        // Khôi phục transient fields
        for (Auction a : loadedAuctions.values()) {
          a.restoreTransients();
        }

        System.out.println("[DataManager] Đã load " + loadedAuctions.size() + " auctions");
      }
    } finally {
      auctionsLock.writeLock().unlock();
    }

    // Load history
    historyLock.writeLock().lock();
    try {
      Map<String, List<BidHistoryEntry>> loadedHistory = loadMapFromFile(
          HISTORY_DATA_FILE);
      if (loadedHistory != null) {
        BidHistoryManager.getInstance().setHistoryMap(loadedHistory);
        System.out.println("[DataManager] Đã load history của "
            + loadedHistory.size() + " users");
      }
    } finally {
      historyLock.writeLock().unlock();
    }

    System.out.println("[DataManager] Load dữ liệu hoàn tất");
  }

  /**
   * Atomic write: Ghi vào file tạm rồi đổi tên.
   */
  private void saveMapToFile(Map<?, ?> data, String fileName) {
    String tempFileName = fileName + TEMP_EXT;
    File tempFile = new File(tempFileName);

    try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(tempFile))) {
      oos.writeObject(data);
      oos.flush();
    } catch (IOException e) {
      System.err.println("[DataManager] Lỗi ghi " + fileName + ": " + e.getMessage());
      if (tempFile.exists()) {
        tempFile.delete();
      }
      throw new RuntimeException("Save failed", e);
    }

    // Move file atomically
    try {
      Path source = Paths.get(tempFileName);
      Path target = Paths.get(fileName);
      Files.move(source, target,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      System.err.println("[DataManager] Lỗi move file " + fileName + ": " + e.getMessage());
      if (tempFile.exists()) {
        tempFile.delete();
      }
      throw new RuntimeException("Atomic move failed", e);
    }
  }

  /**
   * Load file an toàn.
   */
  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> loadMapFromFile(String fileName) {
    File file = new File(fileName);
    if (!file.exists()) {
      return null;
    }

    try (ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream(file))) {
      return (Map<K, V>) ois.readObject();
    } catch (Exception e) {
      System.err.println("[DataManager] Lỗi load " + fileName + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * Shutdown gracefully - save cuối cùng và dừng scheduler.
   */
  public void shutdown() {
    if (isShutdown.compareAndSet(false, true)) {
      System.out.println("[DataManager] Đang shutdown...");

      // Dừng auto-save scheduler
      autoSaveScheduler.shutdown();

      try {
        // Chờ scheduler dừng (tối đa 2 giây)
        if (!autoSaveScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
          autoSaveScheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        autoSaveScheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }

      // Save cuối cùng
      System.out.println("[DataManager] Thực hiện save cuối cùng...");

      // Đánh dấu tất cả là dirty để force save
      usersDirty.set(true);
      auctionsDirty.set(true);
      historyDirty.set(true);

      // Save tất cả
      saveData();

      System.out.println("[DataManager] Shutdown hoàn tất");
    }
  }

  /**
   * Đăng ký shutdown hook để save trước khi JVM tắt.
   */
  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (!isShutdown.get()) {
        System.out.println("[DataManager] JVM đang tắt, thực hiện save cuối...");
        shutdown();
      }
    }, "DataManager-ShutdownHook"));
  }

}