package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.service.auctionservice.AuctionService;

/**
 * interface cac lenh thuc thi.
 */
public interface ClientCommand {
  /**
   * ham excute.
   */
  void execute(String[] parts, ClientHandler client, AuctionService auctionService);
}