package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.dto.AuctionRow;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import java.util.ArrayList;
import java.util.List;

/**
 * xem danh sach theo doi.
 */
public class GetWatchlistCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    if (!(client.getCurrentUser() instanceof Bidder)) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Bạn chưa đăng nhập hoặc không phải bidder.");
      return;
    }

    List<Auction> watchlist = auctionService
        .getWatchlistForUser(client.getCurrentUser().getUsername());
    List<AuctionRow> dtoList = new ArrayList<>();

    for (Auction a : watchlist) {
      dtoList.add(new AuctionRow(a));
    }
    client.sendMessage(Protocol.RES_WATCHLIST + Protocol.SEPARATOR + client.gson.toJson(dtoList));
  }
}