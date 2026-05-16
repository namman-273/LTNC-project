package com.auction.service.auctionservice;

import java.util.List;

import com.auction.model.entities.Auction;
import com.auction.model.enums.AuctionStatus;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.PaymentProcessor.WinnerInfo;
import com.auction.service.bidhistorymanager.BidHistoryManager;
import com.auction.util.core.IDataStorage;

/**
 * Service xử lý logic kết thúc auction.
 * Tuân thủ Single Responsibility Principle.
 */
public class AuctionEndHandler {

  private final AuctionRepository auctionRepository;
  private final AuctionScheduler scheduler;
  private final PaymentProcessor paymentProcessor;
  private final AuctionNotificationService notificationService;
  private final IDataStorage dataStorage;

  public AuctionEndHandler(AuctionRepository auctionRepository,
      AuctionScheduler scheduler,
      PaymentProcessor paymentProcessor,
      AuctionNotificationService notificationService,
      IDataStorage dataStorage) {
    this.auctionRepository = auctionRepository;
    this.scheduler = scheduler;
    this.paymentProcessor = paymentProcessor;
    this.notificationService = notificationService;
    this.dataStorage = dataStorage;
  }

  /**
   * Xử lý kết thúc auction với anti-sniping check.
   */
  public void endAuction(String auctionId) {
    endAuction(auctionId, false);
  }

  /**
   * Xử lý kết thúc auction bởi Admin (bỏ qua check thời gian).
   */
  public void endAuctionByAdmin(String auctionId) {
    endAuction(auctionId, true);
  }

  /**
   * Xử lý kết thúc auction với tùy chọn forced by Admin.
   * 
   * @param auctionId     ID của auction
   * @param forcedByAdmin true nếu Admin đóng sớm, false nếu tự động
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

      // Khóa auction bằng cách set FINISHED
      auction.setStatus(AuctionStatus.FINISHED);

      WinnerInfo winnerInfo;

      // Nếu Admin đóng sớm → Hoàn tiền
      if (forcedByAdmin && System.currentTimeMillis() < auction.getEndTime()) {
        winnerInfo = paymentProcessor.processRefund(auction);

        // Gửi thông báo đặc biệt cho tất cả participants
        String cancelMsg = Protocol.NOTI_AUCTION_CANCELLED + Protocol.SEPARATOR
            + "Phiên " + auctionId + " đã bị đóng sớm bởi Admin";
        notificationService.notifyAll(auction, cancelMsg);

        System.out.println("[ADMIN] Phiên " + auctionId
            + " bị đóng sớm. Đã hoàn tiền cho người dẫn đầu.");
      } else {
        // Kết thúc bình thường → Xử lý thanh toán
        winnerInfo = paymentProcessor.processPayment(auction);
        notificationService.notifyAuctionEnd(auction, winnerInfo);
      }

      // Lưu lịch sử cho tất cả người đặt giá
      String winner = (winnerInfo != null && winnerInfo.hasWinner())
          ? winnerInfo.getWinner().getUsername()
          : null;
      List<String> participants = auction.getBidHistory().stream()
          .map(bid -> bid.getBidder().getUsername())
          .distinct()
          .collect(java.util.stream.Collectors.toList());
      BidHistoryManager.getInstance().recordHistory(
          auction.getId(),
          auction.getItem().getItemName(),
          auction.getCurrentPrice(),
          java.time.LocalDateTime.now().format(
              java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
          winner,
          participants);

      // Giải phóng tài nguyên
      auction.closeAuction();

      // Lưu dữ liệu
      saveData();

      logAuctionEnd(auctionId, auction.getStatus());
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
        || auction.getStatus() == AuctionStatus.PAID;
  }

  /**
   * Lưu dữ liệu xuống storage.
   */
  private void saveData() {
    if (dataStorage != null) {
      dataStorage.saveData();
    }
  }

  /**
   * Log thông tin kết thúc auction.
   */
  private void logAuctionEnd(String auctionId, AuctionStatus status) {
    System.out.println("[FINANCIAL SYSTEM] Phiên " + auctionId
        + " hoàn tất. Trạng thái cuối: " + status);
  }
}