package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;

public class DeleteAuctionCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Cần ít nhất 2 phần: Lệnh | Mã Auction
    if (!client.validatePayload(parts, 2))
      return;

    // Kiểm tra quyền Admin
    if (client.getCurrentUser() == null || !"ADMIN".equals(client.getCurrentUser().getRole())) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Chỉ Admin mới có quyền xóa phiên.");
      return;
    }

    String auctionId = parts[1];
    boolean deleted = auctionService.deleteAuction(auctionId);

    if (deleted) {
      client.sendMessage(Protocol.RES_DELETE_SUCCESS + Protocol.SEPARATOR
          + "Đã xóa phiên " + auctionId);
    } else {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Không tìm thấy phiên hoặc xóa thất bại!");
    }
  }
}