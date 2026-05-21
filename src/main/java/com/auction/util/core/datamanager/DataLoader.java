package com.auction.util.core.datamanager;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.User;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.bidhistorymanager.BidHistoryManager;
import com.auction.service.usermanger.UserManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Quản lý load operations cho từng loại data.
 * Tuân thủ Single Responsibility Principle - chỉ làm việc load data.
 */
public class DataLoader {
  
  private final DataPersistence<String, User> userPersistence;
  private final DataPersistence<String, Auction> auctionPersistence;
  private final DataPersistence<String, List<BidHistoryEntry>> historyPersistence;
  
  // Lock riêng cho từng loại data
  private final ReadWriteLock usersLock = new ReentrantReadWriteLock();
  private final ReadWriteLock auctionsLock = new ReentrantReadWriteLock();
  private final ReadWriteLock historyLock = new ReentrantReadWriteLock();

  /**
   * Constructor với dependency injection (DIP).
   */
  public DataLoader(
      DataPersistence<String, User> userPersistence,
      DataPersistence<String, Auction> auctionPersistence,
      DataPersistence<String, List<BidHistoryEntry>> historyPersistence) {
    this.userPersistence = userPersistence;
    this.auctionPersistence = auctionPersistence;
    this.historyPersistence = historyPersistence;
  }

  /**
   * Load users data.
   */
  public void loadUsers() {
    usersLock.writeLock().lock();
    try {
      Map<String, User> loadedUsers = userPersistence.load("users");
      if (loadedUsers != null) {
        UserManager.getInstance().setUsers(loadedUsers);
        System.out.println("[DataLoader] Loaded " + loadedUsers.size() + " users");
      } else {
        System.out.println("[DataLoader] No users data found");
      }
    } catch (Exception e) {
      System.err.println("[DataLoader] Failed to load users: " + e.getMessage());
    } finally {
      usersLock.writeLock().unlock();
    }
  }

  /**
   * Load auctions data.
   */
  public void loadAuctions() {
    auctionsLock.writeLock().lock();
    try {
      Map<String, Auction> loadedAuctions = auctionPersistence.load("auctions");
      if (loadedAuctions != null) {
        AuctionService.getInstance().setAuctions(loadedAuctions);
        
        // Khôi phục transient fields
        for (Auction auction : loadedAuctions.values()) {
          auction.restoreTransients();
        }
        
        System.out.println("[DataLoader] Loaded " + loadedAuctions.size() + " auctions");
      } else {
        System.out.println("[DataLoader] No auctions data found");
      }
    } catch (Exception e) {
      System.err.println("[DataLoader] Failed to load auctions: " + e.getMessage());
    } finally {
      auctionsLock.writeLock().unlock();
    }
  }

  /**
   * Load history data.
   */
  public void loadHistory() {
    historyLock.writeLock().lock();
    try {
      Map<String, List<BidHistoryEntry>> loadedHistory = historyPersistence.load("history");
      if (loadedHistory != null) {
        BidHistoryManager.getInstance().setHistoryMap(loadedHistory);
        System.out.println("[DataLoader] Loaded history for " 
            + loadedHistory.size() + " users");
      } else {
        System.out.println("[DataLoader] No history data found");
      }
    } catch (Exception e) {
      System.err.println("[DataLoader] Failed to load history: " + e.getMessage());
    } finally {
      historyLock.writeLock().unlock();
    }
  }

  /**
   * Load tất cả data.
   */
  public void loadAll() {
    System.out.println("[DataLoader] Loading all data...");
    
    loadUsers();
    loadAuctions();
    loadHistory();
    
    System.out.println("[DataLoader] Load complete");
  }
}