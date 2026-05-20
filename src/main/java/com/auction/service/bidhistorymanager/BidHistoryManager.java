package com.auction.service.bidhistorymanager;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.util.core.DataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý lịch sử đấu giá.
 * Thread-safe với ConcurrentHashMap.
 */
public class BidHistoryManager {
  private Map<String, List<BidHistoryEntry>> userHistory = new ConcurrentHashMap<>();

  /**
   * Singleton pattern.
   */
  private static class Holder {
    private static final BidHistoryManager INSTANCE = new BidHistoryManager();
  }

  public static BidHistoryManager getInstance() {
    return Holder.INSTANCE;
  }

  public Map<String, List<BidHistoryEntry>> getHistoryMap() {
    return userHistory;
  }

  public void setHistoryMap(Map<String, List<BidHistoryEntry>> map) {
    this.userHistory = map;
  }

  /**
   * Lấy lịch sử đấu giá của 1 user.
   */
  public List<BidHistoryEntry> getHistoryForUser(String username) {
    // Trả về list lịch sử, nếu chưa có thì trả về list rỗng thay vì null
    return userHistory.getOrDefault(username, new ArrayList<>());
  }

  /**
   * Lưu lịch sử đấu giá.
   * Sử dụng mark dirty thay vì save ngay lập tức để tối ưu performance.
   */
  public void recordHistory(String auctionId, String itemName, double finalPrice,
      String endTime, String winner, List<String> participants) {

    // Thêm entry cho mỗi participant
    for (String user : participants) {
      String result = user.equals(winner) ? "WIN" : "LOSE";
      userHistory.computeIfAbsent(user, k -> new ArrayList<>())
          .add(new BidHistoryEntry(auctionId, itemName, finalPrice, result, endTime));
    }

    // Đánh dấu history cần save (sẽ auto-save sau 5 giây)
    DataManager.getInstance().markHistoryDirty();
  }
}