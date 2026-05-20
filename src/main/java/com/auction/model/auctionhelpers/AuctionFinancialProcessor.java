package com.auction.model.auctionhelpers;

import com.auction.model.entities.Auction;
import com.auction.model.entities.BidTransaction;
import com.auction.model.entities.user.User;
import com.auction.network.protocol.Protocol;
import com.auction.util.exception.InvalidBidException;

/**
 * xu ly tai chinh.
 */
public class AuctionFinancialProcessor {

  /**
   * interface method ap dung de update tinh trang dau gia.
   */
  @FunctionalInterface
  public interface StateUpdater {
    void applyState(double newPrice, BidTransaction newTransaction);
  }

  /**
   * xu li giao dich(su dung lambda khi goi method nay o auction).
   */
  public void processTransaction(Auction auction, User bidder, double amount,
      User previousBidder, StateUpdater updater)
      throws InvalidBidException {
    if (bidder.getUsername().equals(auction.getSellerId())) {
      throw new InvalidBidException("Bạn không thể đấu giá sản phẩm của chính mình!");
    }
    if (Double.isNaN(amount) || Double.isInfinite(amount)) {
      throw new InvalidBidException("Giá đặt không hợp lệ (NaN/Infinite)");
    }
    // TRỪ TIỀN TẠM GIỮ CỦA NGƯỜI MỚI
    if (!bidder.deductBalance(amount)) {
      throw new InvalidBidException("Số dư tài khoản không đủ để đặt mức giá này!");
    }

    if (previousBidder != null && !previousBidder.equals(bidder)) {
      // Refund previous bidder (only after new bidder's money is secured)
      BidTransaction lastTransaction = auction.getBidHistory()
          .get(auction.getBidHistory().size() - 1);
      double refundAmount = lastTransaction.getAmount();
      previousBidder.addBalance(refundAmount);

      String outbidMessage = Protocol.NOTI_OUTBID + Protocol.SEPARATOR + auction.getId()
          + Protocol.SEPARATOR + bidder.getUsername() + Protocol.SEPARATOR + amount;
      auction.notifySpecificUser(previousBidder.getUsername(), outbidMessage);

      String refundMessage = Protocol.NOTI_REFUND + Protocol.SEPARATOR + auction.getId()
          + Protocol.SEPARATOR + refundAmount + Protocol.SEPARATOR + previousBidder.getBalance();
      auction.notifySpecificUser(previousBidder.getUsername(), refundMessage);
    }

    updater.applyState(amount, new BidTransaction(bidder, amount));

    String bidUpdateMessage = Protocol.NOTI_BID_UPDATE + Protocol.SEPARATOR + auction.getId()
        + "|" + amount + "|" + bidder.getUsername() + "|"
        + auction.getItem().getClass().getSimpleName();
    auction.notifyAllParticipants(bidUpdateMessage, bidder);
  }
}
