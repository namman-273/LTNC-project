package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;

public class GetBalanceCommand implements ClientCommand {
    @Override
    public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
        // Kiểm tra trạng thái đăng nhập
        if (client.getCurrentUser() == null) {
            client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Bạn chưa đăng nhập.");
            return;
        }
        
        // Lấy số dư và gửi về cho client
        client.sendMessage(Protocol.RES_BALANCE_INFO + Protocol.SEPARATOR 
                + client.getCurrentUser().getBalance());
    }
}