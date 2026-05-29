package com.auction.util.core.datamanager;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.User;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.bidhistorymanager.BidHistoryManager;
import com.auction.service.usermanger.UserManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Quản lý save operations cho từng loại data.
 * Sử dụng lock riêng biệt và copy-on-write để tối ưu performance.
 * Tuân thủ Single Responsibility Principle - chỉ làm việc save data.
 */
public class DataSaver {

  private final DataPersistence<String, User> userPersistence;
  private final DataPersistence<String, Auction> auctionPersistence;
  private final DataPersistence<String, List<BidHistoryEntry>> historyPersistence;

  private final DirtyFlagTracker dirtyTracker;

  // Lock riêng cho từng loại data
  private final ReentrantReadWriteLock usersLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock auctionsLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock historyLock = new ReentrantReadWriteLock();

  /**
   * Constructor với dependency injection (DIP).
   */
  public DataSaver(
      DataPersistence<String, User> userPersistence,
      DataPersistence<String, Auction> auctionPersistence,
      DataPersistence<String, List<BidHistoryEntry>> historyPersistence,
      DirtyFlagTracker dirtyTracker) {
    this.userPersistence = userPersistence;
    this.auctionPersistence = auctionPersistence;
    this.historyPersistence = historyPersistence;
    this.dirtyTracker = dirtyTracker;
  }

  /**
   * Save users data nếu dirty.
   */
  public boolean saveUsersIfDirty() {
    if (!dirtyTracker.clearUsersDirty()) {
      return false;
    }

    usersLock.writeLock().lock();
    try {
      // Copy-on-write: Copy data để giảm thời gian hold lock
      Map<String, User> snapshot = new HashMap<>(
          UserManager.getInstance().getUsers());

      // Release lock sớm
      usersLock.writeLock().unlock();

      // Serialize và save (không cần lock)
      userPersistence.save(snapshot, "users");
      return true;

    } catch (Exception e) {
      System.err.println("[DataSaver] Failed to save users: " + e.getMessage());
      dirtyTracker.markUsersDirty(); // Retry lần sau
      return false;
    } finally {
      if (usersLock.writeLock().isHeldByCurrentThread()) {
        usersLock.writeLock().unlock();
      }
    }
  }

  /**
   * Save auctions data nếu dirty.
   */
  public boolean saveAuctionsIfDirty() {
    if (!dirtyTracker.clearAuctionsDirty()) {
      return false;
    }

    auctionsLock.writeLock().lock();
    try {
      Map<String, Auction> snapshot = new HashMap<>(
          AuctionService.getInstance().getAuctionsMap());

      auctionsLock.writeLock().unlock();

      auctionPersistence.save(snapshot, "auctions");
      return true;

    } catch (Exception e) {
      System.err.println("[DataSaver] Failed to save auctions: " + e.getMessage());
      dirtyTracker.markAuctionsDirty();
      return false;
    } finally {
      if (auctionsLock.writeLock().isHeldByCurrentThread()) {
        auctionsLock.writeLock().unlock();
      }
    }
  }

  /**
   * Save history data nếu dirty.
   */
  public boolean saveHistoryIfDirty() {
    if (!dirtyTracker.clearHistoryDirty()) {
      return false;
    }

    historyLock.writeLock().lock();
    try {
      Map<String, List<BidHistoryEntry>> snapshot = new HashMap<>(
          BidHistoryManager.getInstance().getHistoryMap());

      historyLock.writeLock().unlock();

      historyPersistence.save(snapshot, "history");
      return true;

    } catch (Exception e) {
      System.err.println("[DataSaver] Failed to save history: " + e.getMessage());
      dirtyTracker.markHistoryDirty();
      return false;
    } finally {
      if (historyLock.writeLock().isHeldByCurrentThread()) {
        historyLock.writeLock().unlock();
      }
    }
  }

  /**
   * Save tất cả data (bất kể dirty hay không).
   */
  public void saveAll() {
    dirtyTracker.markAllDirty();

    boolean usersSaved = saveUsersIfDirty();
    boolean auctionsSaved = saveAuctionsIfDirty();
    boolean historySaved = saveHistoryIfDirty();

    if (usersSaved || auctionsSaved || historySaved) {
      System.out.println("[DataSaver] Saved: users=" + usersSaved
          + ", auctions=" + auctionsSaved + ", history=" + historySaved);
    }
  }

  /**
   * Save tất cả data dirty.
   */
  public void saveAllDirty() {
    boolean usersSaved = saveUsersIfDirty();
    boolean auctionsSaved = saveAuctionsIfDirty();
    boolean historySaved = saveHistoryIfDirty();

    if (usersSaved || auctionsSaved || historySaved) {
      System.out.println("[DataSaver] Auto-save complete");
    }
  }
}