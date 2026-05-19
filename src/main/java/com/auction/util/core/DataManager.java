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
import java.util.List;
import java.util.Map;

/**
 *  * lưu thông tin xuống dạng file.
 *  
 */

public final class DataManager implements IdataStorage {
  private static final String AUCTION_DATA_FILE = "auctions.dat";
  private static final String USER_DATA_FILE = "users.dat";
  private static final String TEMP_EXT = ".tmp";
  private static final String HISTORY_DATA_FILE = "history.dat";

  // Singleton Pattern để giải quyết lỗi
  private DataManager() {
  }

  /**
   *  * Áp dụng singleton.
   *  
   */
  private static class Holder {
    private static final DataManager INSTANCE = new DataManager();
  }

  // 3. Hàm getInstance không còn synchronized, không còn if-else
  public static DataManager getInstance() {
    return Holder.INSTANCE;
  }

  /**
   * Lưu toàn bộ dữ liệu hệ thống .
   */
  public synchronized void saveData() {
    saveMapToFile(UserManager.getInstance().getUsers(), USER_DATA_FILE);
    saveMapToFile(AuctionService.getInstance().getAuctionsMap(), AUCTION_DATA_FILE);
    saveMapToFile(BidHistoryManager.getInstance().getHistoryMap(), HISTORY_DATA_FILE);
  }

  /**
   * Tải toàn bộ dữ liệu hệ thống khi khởi động Server.
   */
  public synchronized void loadData() {
    // Tải người dùng
    Map<String, User> loadedUsers = loadMapFromFile(USER_DATA_FILE);
    if (loadedUsers != null) {
      UserManager.getInstance().setUsers(loadedUsers);
    }

    // Tải phiên đấu giá
    Map<String, Auction> loadedAuctions = loadMapFromFile(AUCTION_DATA_FILE);
    if (loadedAuctions != null) {
      AuctionService.getInstance().setAuctions(loadedAuctions);
      // Quan trọng: Sau khi load Auction, phải khôi phục transient (lock,
      // executor...)
      for (Auction a : loadedAuctions.values()) {
        a.restoreTransients();
      }
    }
    // Tải lịch sử đấu giá
    Map<String, List<BidHistoryEntry>> loadedHistory = loadMapFromFile(
        HISTORY_DATA_FILE);
    if (loadedHistory != null) {
      BidHistoryManager.getInstance().setHistoryMap(loadedHistory);
    }
  }

  /**
   * Logic Atomic Write: Ghi vào file tạm rồi mới đổi tên để tránh hỏng dữ liệu.
   */
  private void saveMapToFile(Map<?, ?> data, String fileName) {
    String tempFileName = fileName + TEMP_EXT;
    File tempFile = new File(tempFileName);

    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
      oos.writeObject(data);
      oos.flush();

      // Đóng stream trước khi move file
      oos.close();

      Path source = Paths.get(tempFileName);
      Path target = Paths.get(fileName);
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      System.err.println(">>> [DataManager] Lỗi lưu " + fileName + ": " + e.getMessage());
      if (tempFile.exists()) {
        tempFile.delete();
      }
    }
  }

  /**
   * Logic đọc file an toàn.
   */
  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> loadMapFromFile(String fileName) {
    File file = new File(fileName);
    if (!file.exists()) {
      return null;
    }

    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      return (Map<K, V>) ois.readObject();
    } catch (Exception e) {
      System.err.println(">>> [DataManager] Lỗi nạp " + fileName + ": " + e.getMessage());
      return null;
    }
  }
}
