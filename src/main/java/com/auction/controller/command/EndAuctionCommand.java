package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;

/**
 * Command xử lý việc Admin đóng phiên đấu giá sớm.
 * Khi đóng sớm (còn thời gian), người dẫn đầu sẽ được hoàn tiền.
 */
public class EndAuctionCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Cần ít nhất 2 phần: Lệnh | Mã Auction
    if (!client.validatePayload(parts, 2)) {
      return;
    }

    // Kiểm tra quyền Admin
    if (client.getCurrentUser() == null || !"ADMIN".equals(client.getCurrentUser().getRole())) {
      client.sendMessage(Protocol.ERROR
          + Protocol.SEPARATOR + "Chỉ Admin mới có quyền đóng phiên.");
      return;
    }

    String auctionId = parts[1];

    // Method này sẽ tự động hoàn tiền nếu phiên còn thời gian
    auctionService.endAuctionByAdmin(auctionId);

    client.sendMessage(Protocol.RES_END_SUCCESS + Protocol.SEPARATOR
        + "Đã đóng phiên " + auctionId
        + ". Người dẫn đầu đã được hoàn tiền nếu phiên còn thời gian.");
  }
}