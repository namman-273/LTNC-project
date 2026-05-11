package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;

public class EndAuctionCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Cần ít nhất 2 phần: Lệnh | Mã Auction
    if (!client.validatePayload(parts, 2))
      return;

    // Kiểm tra quyền Admin
    if (client.getCurrentUser() == null || !"ADMIN".equals(client.getCurrentUser().getRole())) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Chỉ Admin mới có quyền đóng phiên.");
      return;
    }

    String auctionId = parts[1];
    auctionService.endAuction(auctionId);
    client.sendMessage(Protocol.RES_END_SUCCESS + Protocol.SEPARATOR + "Đã đóng phiên " + auctionId);
  }
}