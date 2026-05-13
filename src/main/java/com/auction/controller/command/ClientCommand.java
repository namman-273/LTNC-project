package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.service.auctionservice.AuctionService;

public interface ClientCommand {
    void execute(String[] parts, ClientHandler client, AuctionService auctionService);
}