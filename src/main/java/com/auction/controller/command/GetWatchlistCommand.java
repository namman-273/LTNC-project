package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.dto.AuctionRow;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;

import java.util.ArrayList;
import java.util.List;

public class GetWatchlistCommand implements ClientCommand {
    @Override
    public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
        if (!(client.getCurrentUser() instanceof Bidder)) {
            client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
                    + "Bạn chưa đăng nhập hoặc không phải bidder.");
            return;
        }
        
        List<Auction> watchlist = auctionService.getWatchlistForUser(client.getCurrentUser().getUsername());
        List<AuctionRow> dtoList = new ArrayList<>();
        
        for (Auction a : watchlist) {
            dtoList.add(new AuctionRow(
                    a.getId(),
                    a.getItem() != null ? a.getItem().getItemName() : "---",
                    a.getCurrentPrice(),
                    a.getStatus().name(),
                    a.getEndTime(),
                    a.getSellerId()
            ));
        }
        client.sendMessage(Protocol.RES_WATCHLIST + Protocol.SEPARATOR + client.gson.toJson(dtoList));
    }
}