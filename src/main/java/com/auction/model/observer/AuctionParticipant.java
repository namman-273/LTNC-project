package com.auction.model.observer;

public interface AuctionParticipant extends Observer {
  // Chỉ những Observer nào có User thì mới implement interface này
  String getAssociatedUsername();
}
