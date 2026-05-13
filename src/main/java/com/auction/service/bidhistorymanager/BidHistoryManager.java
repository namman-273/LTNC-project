package com.auction.service.bidhistorymanager;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.util.core.DataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BidHistoryManager {
  private static BidHistoryManager instance;
  private Map<String, List<BidHistoryEntry>> userHistory = new HashMap<>();

  public static synchronized BidHistoryManager getInstance() {
    if (instance == null)
      instance = new BidHistoryManager();
    return instance;
  }

  public Map<String, List<BidHistoryEntry>> getHistoryMap() {
    return userHistory;
  }

  public void setHistoryMap(Map<String, List<BidHistoryEntry>> map) {
    this.userHistory = map;
  }

  public List<BidHistoryEntry> getHistoryForUser(String username) {
    // Trả về list lịch sử, nếu chưa có thì trả về list rỗng thay vì null để tránh
    // lỗi crash
    return userHistory.getOrDefault(username, new ArrayList<>());
  }

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