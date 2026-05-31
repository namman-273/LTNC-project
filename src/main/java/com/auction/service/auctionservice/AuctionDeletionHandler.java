package com.auction.service.auctionservice;

import com.auction.controller.network.ConnectionManager;
import com.auction.model.entities.Auction;
import com.auction.network.protocol.Protocol;

/**
 * Service xử lý logic xóa auction (Admin only).
 */

public class AuctionDeletionHandler {

  private final AuctionRepository auctionRepository;
  private final PaymentProcessor paymentProcessor;
  private final AuctionDataPersistenceService persistenceService;

  /**
   * Constructor với dependency injection.
   */
  public AuctionDeletionHandler(
      AuctionRepository auctionRepository,
      PaymentProcessor paymentProcessor,
      AuctionDataPersistenceService persistenceService) {
    this.auctionRepository = auctionRepository;
    this.paymentProcessor = paymentProcessor;
    this.persistenceService = persistenceService;
  }

  /**
   * Xóa auction - chỉ Admin mới được phép.
   */
  public boolean deleteAuction(String auctionId) {
    Auction auction = auctionRepository.findById(auctionId);

    // Kiểm tra auction có tồn tại không
    if (auction == null) {
      System.err.println("[DELETE ERROR] Auction không tồn tại: " + auctionId);
      return false;
    }

    // Synchronized để đảm bảo thread-safe
    synchronized (auction) {
      // Bước 1: Hoàn tiền cho người dẫn đầu (nếu có)
      paymentProcessor.processRefund(auction);

      String cancelMsg = Protocol.NOTI_AUCTION_CANCELLED
          + Protocol.SEPARATOR
          + auctionId
          + Protocol.SEPARATOR
          + "Phiên đã bị Admin xóa";
      ConnectionManager.getInstance().broadcastToAll(cancelMsg);

      // Bước 2: Đóng auction (release observers và resources)
      auction.closeAuction();

      // Bước 3: Xóa khỏi repository
      auctionRepository.remove(auctionId);

      // Bước 4: Đánh dấu cần save data
      persistenceService.markAuctionsDirty();

      // Log kết quả
      System.out.println("[ADMIN] Đã xóa phiên: " + auctionId);

      return true;
    }
  }
}