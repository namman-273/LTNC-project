package com.auction.service.auctionservice;


import com.auction.model.entities.Auction;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.PaymentProcessor.WinnerInfo;

/**
 * Service xử lý notifications cho auctions.
 * Tuân thủ Single Responsibility Principle.
 */
public class AuctionNotificationService {

  /**
   * Gửi thông báo khi auction kết thúc.
   */
  public void notifyAuctionEnd(Auction auction, WinnerInfo winnerInfo) {
    if (auction == null) {
      return;
    }

    String message = buildEndMessage(auction.getId(), winnerInfo);
    auction.notifyAllParticipants(message, null);
  }

  /**
   * Build message thông báo kết thúc auction.
   */
  private String buildEndMessage(String auctionId, WinnerInfo winnerInfo) {
    if (winnerInfo != null && winnerInfo.hasWinner()) {
      return Protocol.RES_END_SUCCESS + Protocol.SEPARATOR + auctionId
          + Protocol.SEPARATOR + "Winner:" + winnerInfo.getWinner().getUsername()
          + Protocol.SEPARATOR + "Bid:" + winnerInfo.getWinningPrice() + "$";
    } else {
      return Protocol.RES_END_SUCCESS + Protocol.SEPARATOR + auctionId
          + Protocol.SEPARATOR + "No winner";
    }
  }

  /**
   * Gửi thông báo tùy chỉnh cho tất cả participants.
   */
  public void notifyAll(Auction auction, String message) {
    if (auction != null) {
      auction.notifyAllParticipants(message, null);
    }
  }

  /**
   * Gửi thông báo cho một user cụ thể.
   */
  public void notifySpecificUser(Auction auction, String username, String message) {
    if (auction != null) {
      auction.notifySpecificUser(username, message);
    }
  }
}
