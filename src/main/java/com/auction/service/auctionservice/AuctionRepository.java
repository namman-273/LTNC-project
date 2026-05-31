package com.auction.service.auctionservice;

import com.auction.model.entities.Auction;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository pattern để quản lý storage của auctions.
 * Tuân thủ Single Responsibility Principle.
 */
public class AuctionRepository {

  private final Map<String, Auction> auctions = new ConcurrentHashMap<>();

  /**
   * Lấy toàn bộ Map auctions.
   */
  public Map<String, Auction> getAll() {
    return auctions;
  }

  /**
   * Thêm auction mới.
   */
  public void add(String auctionId, Auction auction) {
    auctions.put(auctionId, auction);
  }

  /**
   * Tìm auction theo ID.
   */
  public Auction findById(String auctionId) {
    return auctions.get(auctionId);
  }

  /**
   * Lấy tất cả auctions dưới dạng Collection.
   */
  public Collection<Auction> getAllAuctions() {
    return auctions.values();
  }

  /**
   * Kiểm tra auction có tồn tại không.
   */
  public boolean exists(String auctionId) {
    return auctions.containsKey(auctionId);
  }

  /**
   * Xóa auction.
   */
  public boolean remove(String auctionId) {
    return auctions.remove(auctionId) != null;
  }

  /**
   * Xóa toàn bộ và load lại từ file.
   */
  public void replaceAll(Map<String, Auction> loadedAuctions) {
    if (loadedAuctions != null) {
      auctions.clear();
      auctions.putAll(loadedAuctions);
    }
  }

  /**
   * Kiểm tra repository có rỗng không.
   */
  public boolean isEmpty() {
    return auctions.isEmpty();
  }
}
