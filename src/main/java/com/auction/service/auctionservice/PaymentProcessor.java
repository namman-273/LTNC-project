package com.auction.service.auctionservice;

import com.auction.controller.network.ConnectionManager;
import com.auction.model.entities.Auction;
import com.auction.model.entities.BidTransaction;
import com.auction.model.entities.user.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.network.protocol.Protocol;
import com.auction.service.usermanger.UserManager;
import java.util.List;

/**
 * Service xử lý payment và financial transactions.
 * Tuân thủ Single Responsibility Principle.
 */
public class PaymentProcessor {

  /**
   * Xử lý thanh toán khi auction kết thúc BÌNH THƯỜNG (không bị Admin đóng sớm).
   * 
   * LOGIC:
   * - Có winner → Cộng tiền cho seller → Set status = PAID
   * - Không có winner → Giữ nguyên status = FINISHED
   * 
   * @return WinnerInfo chứa thông tin winner và giá thắng
   */
  public WinnerInfo processPayment(Auction auction) {
    if (auction == null) {
      return null;
    }

    // Xác định winner từ BidHistory
    List<BidTransaction> history = auction.getBidHistory();
    User winner = null;
    double maxPrice = 0;

    if (!history.isEmpty()) {
      BidTransaction lastBid = history.get(history.size() - 1);
      winner = lastBid.getBidder();
      maxPrice = lastBid.getAmount();
    }

    // ===== XỬ LÝ THANH TOÁN NẾU CÓ WINNER =====
    if (winner != null) {
      User seller = UserManager.getInstance().findUserByUsername(auction.getSellerId());

      if (seller != null) {
        // 1. Cộng tiền cho người bán
        seller.addBalance(maxPrice);

        // 2. CẬP NHẬT TRẠNG THÁI → PAID (thanh toán thành công)
        auction.setStatus(AuctionStatus.PAID);

        // 3. Thông báo balance changed cho seller
        notifySellerBalanceChanged(auction, seller, maxPrice);

        System.out.println("[PAYMENT]  Đã chuyển " + maxPrice + "$ cho seller: "
            + seller.getUsername() + " - Trạng thái: PAID");
      } else {
        System.err.println("[PAYMENT ERROR]  Không tìm thấy seller: "
            + auction.getSellerId() + " - Trạng thái: FINISHED");
        // Trạng thái vẫn là FINISHED vì không thanh toán được
      }
    } else {
      System.out.println("[PAYMENT]  Không có winner - Trạng thái: FINISHED");
      // Trạng thái vẫn là FINISHED vì không có người thắng
    }

    return new WinnerInfo(winner, maxPrice);
  }

  /**
   * Hoàn tiền cho người dẫn đầu khi auction bị Admin đóng sớm.
   * 
   * LOGIC:
   * - Tìm người dẫn đầu → Hoàn tiền
   * - Set status = CANCELED
   * - Return WinnerInfo(null, 0) vì KHÔNG CÓ WINNER (chỉ có refund)
   * 
   * @return WinnerInfo với winner=null và price=0
   */
  public WinnerInfo processRefund(Auction auction) {
    if (auction == null) {
      return null;
    }

    // Xác định người dẫn đầu từ BidHistory
    List<BidTransaction> history = auction.getBidHistory();
    User leadingBidder = null;
    double refundAmount = 0;

    if (!history.isEmpty()) {
      BidTransaction lastBid = history.get(history.size() - 1);
      leadingBidder = lastBid.getBidder();
      refundAmount = lastBid.getAmount();

      // Hoàn tiền cho người dẫn đầu
      leadingBidder.addBalance(refundAmount);

      // Gửi thông báo hoàn tiền
      String refundMsg = Protocol.NOTI_REFUND + Protocol.SEPARATOR
          + auction.getId() + Protocol.SEPARATOR
          + refundAmount + Protocol.SEPARATOR
          + "Phiên đấu giá đã bị đóng bởi Admin";

      auction.notifySpecificUser(leadingBidder.getUsername(), refundMsg);

      System.out.println("[REFUND]   Đã hoàn " + refundAmount + "$ cho user: "
          + leadingBidder.getUsername());
    }

    // Set status = CANCELED (phiên bị hủy bởi Admin)
    auction.setStatus(AuctionStatus.CANCELED);

    System.out.println("[REFUND] Phiên bị Admin đóng sớm - Trạng thái: CANCELED");

    // QUAN TRỌNG: Return WinnerInfo(null, 0) vì KHÔNG có winner
    // (Admin đóng sớm = không có người thắng thật sự)
    return new WinnerInfo(null, 0);
  }

  /**
   * Thông báo cho seller về việc balance thay đổi.
   * Sử dụng ConnectionManager để gửi trực tiếp, không qua observer pattern.
   */
  private void notifySellerBalanceChanged(Auction auction, User seller, double amount) {
    String sellerMsg = Protocol.NOTI_BALANCE_CHANGED + Protocol.SEPARATOR
        + auction.getId() + Protocol.SEPARATOR + seller.getBalance() + Protocol.SEPARATOR
        + "+" + amount;

    // Gửi thông báo trực tiếp cho seller thông qua ConnectionManager
    boolean sent = ConnectionManager.getInstance().sendDirectMessage(
        seller.getUsername(), sellerMsg);

    if (sent) {
      System.out.println("[NOTIFICATION]  Đã gửi thông báo balance changed cho seller: "
          + seller.getUsername());
    } else {
      System.out.println("[NOTIFICATION]   Seller " + seller.getUsername()
          + " không online, bỏ qua thông báo");
    }
  }

  /**
   * Inner class để lưu thông tin winner.
   */
  public static class WinnerInfo {
    private final User winner;
    private final double winningPrice;

    public WinnerInfo(User winner, double winningPrice) {
      this.winner = winner;
      this.winningPrice = winningPrice;
    }

    public User getWinner() {
      return winner;
    }

    public double getWinningPrice() {
      return winningPrice;
    }

    public boolean hasWinner() {
      return winner != null;
    }
  }
}