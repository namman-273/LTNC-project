package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.controller.network.ConnectionManager;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;

/**
 *  * kết thúc phiên sớm.
 *  
 */
public class EndAuctionCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    if (!client.validatePayload(parts, 2)) {
      return;
    }

    if (client.getCurrentUser() == null || !"ADMIN".equals(client.getCurrentUser().getRole())) {
      client.sendMessage(Protocol.ERROR
          + Protocol.SEPARATOR + "Chỉ Admin mới có quyền đóng phiên.");
      return;
    }

    String auctionId = parts[1];
    auctionService.endAuctionByAdmin(auctionId);

    client.sendMessage(Protocol.RES_ADMIN_END_SUCCESS + Protocol.SEPARATOR
        + "Đã đóng phiên " + auctionId
        + ". Người dẫn đầu đã được hoàn tiền nếu phiên còn thời gian.");
    // Broadcast cho tất cả client để cập nhật danh sách
    String broadcast = Protocol.NOTI_AUCTION_CANCELLED + Protocol.SEPARATOR
        + auctionId + Protocol.SEPARATOR + "Phiên đã bị Admin đóng sớm";
    ConnectionManager.getInstance().broadcastToAll(broadcast);
  }
}