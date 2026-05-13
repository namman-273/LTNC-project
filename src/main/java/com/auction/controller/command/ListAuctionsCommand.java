package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.dto.AuctionRow;
import com.auction.model.entities.Auction;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;

import java.util.ArrayList;
import java.util.List;

public class ListAuctionsCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    List<AuctionRow> dtoList = new ArrayList<>();
    for (Auction a : auctionService.getAllAuctions()) {
      dtoList.add(new AuctionRow(a));
    }
    // Gọi client.gson để parse JSON
    client.sendMessage(Protocol.RES_LIST_SUCCESS + Protocol.SEPARATOR + client.gson.toJson(dtoList));
  }
}
