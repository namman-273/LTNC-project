package com.auction.service.auctionservice;

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
   * Xử lý thanh toán khi auction kết thúc.
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

    // Xử lý thanh toán nếu có winner
    if (winner != null) {
      User seller = UserManager.getInstance().findUserByUsername(auction.getSellerId());

      if (seller != null) {
        // Cộng tiền cho người bán
        seller.addBalance(maxPrice);

        // Cập nhật trạng thái auction
        auction.setStatus(AuctionStatus.PAID);

        // Thông báo balance changed cho seller
        notifySellerBalanceChanged(auction, seller, maxPrice);

        System.out.println("[PAYMENT] Đã chuyển " + maxPrice + "$ cho seller: "
            + seller.getUsername());
      } else {
        System.err.println("[PAYMENT ERROR] Không tìm thấy seller: " + auction.getSellerId());
      }
    }

    return new WinnerInfo(winner, maxPrice);
  }

  /**
   * Hoàn tiền cho người dẫn đầu khi auction bị hủy sớm.
   * 
   * @return WinnerInfo chứa thông tin người được hoàn tiền
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

      System.out.println("[REFUND] Đã hoàn " + refundAmount + "$ cho user: "
          + leadingBidder.getUsername());
    }

    return new WinnerInfo(leadingBidder, refundAmount);
  }

  /**
   * Thông báo cho seller về việc balance thay đổi.
   */
  private void notifySellerBalanceChanged(Auction auction, User seller, double amount) {
    String sellerMsg = Protocol.NOTI_BALANCE_CHANGED + Protocol.SEPARATOR
        + auction.getId() + Protocol.SEPARATOR + seller.getBalance() + Protocol.SEPARATOR
        + "+" + amount;

    auction.notifySpecificUser(seller.getUsername(), sellerMsg);
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