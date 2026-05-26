package com.auction.service.auctionservice;

import com.auction.model.entities.Auction;
import com.auction.model.enums.AuctionStatus;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.PaymentProcessor.WinnerInfo;
import com.auction.service.bidhistorymanager.BidHistoryManager;
import java.util.List;

/**
 * Service xử lý logic kết thúc auction.
 * Tuân thủ Single Responsibility Principle.
 */
public class AuctionEndHandler {

  private final AuctionRepository auctionRepository;
  private final AuctionScheduler scheduler;
  private final PaymentProcessor paymentProcessor;
  private final AuctionNotificationService notificationService;
  private final AuctionDataPersistenceService persistenceService;

  /**
   * Constructor với dependency injection.
   */
  public AuctionEndHandler(AuctionRepository auctionRepository,
      AuctionScheduler scheduler,
      PaymentProcessor paymentProcessor,
      AuctionNotificationService notificationService,
      AuctionDataPersistenceService persistenceService) {
    this.auctionRepository = auctionRepository;
    this.scheduler = scheduler;
    this.paymentProcessor = paymentProcessor;
    this.notificationService = notificationService;
    this.persistenceService = persistenceService;
  }

  /**
   * Xử lý kết thúc auction bởi Admin (bỏ qua check thời gian).
   */
  public void endAuctionByAdmin(String auctionId) {
    endAuction(auctionId, true);
  }

  /**
   * Xử lý kết thúc auction với anti-sniping check.
   */
  public void endAuction(String auctionId) {
    endAuction(auctionId, false);
  }

  /**
   * Xử lý kết thúc auction với tùy chọn forced by Admin.
   * LOGIC TRẠNG THÁI:
   * - Admin đóng sớm: CANCELED (không có winner, chỉ refund)
   * - Kết thúc bình thường + có winner: PAID (có thanh toán)
   * - Kết thúc bình thường + không có winner: FINISHED (không có giao dịch)
   * forcedByAdmin true nếu Admin đóng sớm, false nếu tự động.
   */
  private void endAuction(String auctionId, boolean forcedByAdmin) {
    Auction auction = auctionRepository.findById(auctionId);
    if (auction == null) {
      return;
    }

    // Synchronized để đảm bảo chỉ có 1 thread xử lý
    synchronized (auction) {
      // Nếu KHÔNG phải Admin đóng sớm, kiểm tra thời gian
      if (!forcedByAdmin && !isTimeToEnd(auction, auctionId)) {
        return;
      }

      // Kiểm tra trạng thái để tránh xử lý 2 lần
      if (isAlreadyFinished(auction)) {
        return;
      }

      // Khóa auction tạm thời bằng cách set FINISHED
      // (Status cuối cùng sẽ được set bởi payment processor)
      auction.setStatus(AuctionStatus.FINISHED);

      WinnerInfo winnerInfo;
      String winner = null;

      // ===== CASE 1: Admin đóng sớm → Hoàn tiền =====
      if (forcedByAdmin && System.currentTimeMillis() < auction.getEndTime()) {
        winnerInfo = paymentProcessor.processRefund(auction);
        // winnerInfo = WinnerInfo(null, 0) - KHÔNG có winner

        // Gửi thông báo đặc biệt cho tất cả participants
        String cancelMsg = Protocol.NOTI_AUCTION_CANCELLED + Protocol.SEPARATOR
            + "Phiên " + auctionId + " đã bị đóng sớm bởi Admin";
        notificationService.notifyAll(auction, cancelMsg);

        // Trạng thái cuối: CANCELED
        // winner = null (không có winner khi bị hủy)
        System.out.println("[ADMIN]  Phiên " + auctionId
            + " bị đóng sớm. Đã hoàn tiền. Trạng thái: CANCELED");

      // ===== CASE 2: Kết thúc bình thường → Xử lý thanh toán =====
      } else {
        winnerInfo = paymentProcessor.processPayment(auction);
        // winnerInfo có thể có winner (PAID) hoặc null (FINISHED)

        notificationService.notifyAuctionEnd(auction, winnerInfo);

        // Extract winner từ winnerInfo
        if (winnerInfo != null && winnerInfo.hasWinner()) {
          winner = winnerInfo.getWinner().getUsername();
          System.out.println("[PAYMENT]  Phiên " + auctionId
              + " kết thúc bình thường. Đã thanh toán. Trạng thái: PAID");
        } else {
          System.out.println("[END]  Phiên " + auctionId
              + " kết thúc không có người thắng. Trạng thái: FINISHED");
        }
      }

      // Lưu lịch sử cho tất cả người đặt giá
      // winner = null nếu admin đóng sớm hoặc không có bid
      List<String> participants = auction.getBidHistory().stream()
          .map(bid -> bid.getBidder().getUsername())
          .distinct()
          .collect(java.util.stream.Collectors.toList());

      // BidHistoryManager sẽ tự mark dirty
      BidHistoryManager.getInstance().recordHistory(
          auction.getId(),
          auction.getItem().getItemName(),
          auction.getCurrentPrice(),
          java.time.LocalDateTime.now().format(
              java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
          winner, // null nếu admin đóng sớm
          participants);

      // Giải phóng tài nguyên (KHÔNG ghi đè status)
      auction.closeAuction();

      // Đánh dấu auctions cần save
      persistenceService.markAuctionsDirty();

      System.out.println("[FINANCIAL SYSTEM]  Phiên " + auctionId
          + " hoàn tất. Trạng thái cuối: " + auction.getStatus());
    }
  }

  /**
   * Kiểm tra đã đến lúc kết thúc auction chưa (xử lý anti-sniping).
   */
  private boolean isTimeToEnd(Auction auction, String auctionId) {
    long now = System.currentTimeMillis();

    if (now < auction.getEndTime()) {
      // Chưa hết giờ, lên lịch lại
      long remaining = auction.getEndTime() - now;
      scheduler.scheduleAuctionEndWithDelay(auctionId, remaining,
          () -> endAuction(auctionId));
      return false;
    }

    return true;
  }

  /**
   * Kiểm tra auction đã kết thúc chưa.
   */
  private boolean isAlreadyFinished(Auction auction) {
    return auction.getStatus() == AuctionStatus.FINISHED
        || auction.getStatus() == AuctionStatus.PAID
        || auction.getStatus() == AuctionStatus.CANCELED;
  }
}