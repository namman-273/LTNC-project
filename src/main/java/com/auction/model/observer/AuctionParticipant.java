package com.auction.model.observer;

/**
 * ap dung isp.
 */
public interface AuctionParticipant extends Observer {
  // Chỉ những Observer nào có User thì mới implement interface này
  String getAssociatedUsername();
}
