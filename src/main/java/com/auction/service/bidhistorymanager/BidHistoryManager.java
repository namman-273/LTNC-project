package com.auction.service.bidhistorymanager;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.util.core.DataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * quan li lich su dau gia.
 */
public class BidHistoryManager {
  private Map<String, List<BidHistoryEntry>> userHistory = new ConcurrentHashMap<>();

  /**
   * singleton.
   */
  // Inner class giữ instance. JVM đảm bảo luồng an toàn khi load class này
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
   * cac du lieu cua 1 user.
   */
  public List<BidHistoryEntry> getHistoryForUser(String username) {
    // Trả về list lịch sử, nếu chưa có thì trả về list rỗng thay vì null để tránh
    // lỗi crash
    return userHistory.getOrDefault(username, new ArrayList<>());
  }

  /**
   * ban luu lich su dau gia.
   */
  public void recordHistory(String auctionId, String itemName, double finalPrice, String endTime,
      String winner, List<String> participants) {
    for (String user : participants) {
      String res = user.equals(winner) ? "WIN" : "LOSE";
      userHistory.computeIfAbsent(user, k -> new ArrayList<>())
          .add(new BidHistoryEntry(auctionId, itemName, finalPrice, res, endTime));
    }
    DataManager.getInstance().saveData(); // Gọi lưu file .dat ngay
  }
}