package com.auction.model.auctionhelpers;

import com.auction.model.entities.Auction;
import com.auction.model.entities.user.User;
import com.auction.network.protocol.Protocol;

public class AuctionSnipingProcessor {

  @FunctionalInterface
  public interface TimeUpdater {
    void applyExtension(long extraTime);
  }

  public void handleAntiSniping(Auction auction, User bidder, long oneMinuteMs, long twoMinutesMs, int maxExt,
      int currentExt, TimeUpdater updater) {
    long timeLeft = auction.getEndTime() - System.currentTimeMillis();
    if (timeLeft > 0 && timeLeft < oneMinuteMs && currentExt < maxExt) {
      updater.applyExtension(twoMinutesMs);

      String message = Protocol.NOTI_SNIPING_UPDATE + Protocol.SEPARATOR + auction.getId()
          + "|" + auction.getEndTime() + "|" + (currentExt + 1);

      auction.notifyAllParticipants(message, null);
      System.out
          .println("[ANTI-SNIPING] Phiên " + auction.getId() + " được gia hạn thêm 2p bởi " + bidder.getUsername());
    }
  }
}