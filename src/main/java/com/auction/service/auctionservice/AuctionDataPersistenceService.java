package com.auction.service.auctionservice;

import com.auction.util.core.datamanager.DataManager;

/**
 * Service để xử lý persistence operations cho AuctionService.
 * Tuân thủ Dependency Inversion Principle - có thể thay đổi implementation dễ
 * dàng.
 */
public class AuctionDataPersistenceService {

  /**
   * Đánh dấu auctions data cần save (sẽ auto-save sau 5 giây).
   */
  public void markAuctionsDirty() {
    DataManager.getInstance().markAuctionsDirty();
  }

  /**
   * Save ngay lập tức (dùng cho critical operations).
   * VD: shutdown, force save
   */
  public void saveDataImmediately() {
    DataManager.getInstance().saveData();
  }
}
