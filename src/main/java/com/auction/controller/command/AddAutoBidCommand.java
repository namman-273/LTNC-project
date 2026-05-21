package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.util.core.datamanager.DataManager;

/**
 * Lệnh thực thi đặt bot.
 */
public class AddAutoBidCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    if (!client.validatePayload(parts, 4)) {
      return; // REQ_AUTO_BID = 4
    }

    // Kiểm tra role
    if (!(client.getCurrentUser() instanceof Bidder)) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Chỉ người mua mới có quyền cài đặt Robot.");
      return;
    }

    try {
      String auctionId = parts[1];
      double maxBid = Double.parseDouble(parts[2]);
      double bidIncrement = Double.parseDouble(parts[3]);

      Auction auction = auctionService.getAuctionById(auctionId);
      if (auction == null) {
        client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá.");
        return;
      }

      // Cấu hình auto bid
      auction.addAutoBidConfig(client.getCurrentUser().getUsername(), maxBid, bidIncrement);

      // Đánh dấu auctions cần save
      DataManager.getInstance().markAuctionsDirty();

      client.sendMessage(Protocol.RES_AUTO_BID_SUCCESS + Protocol.SEPARATOR
          + auctionId + Protocol.SEPARATOR + "Autobid bot đã sẵn sàng với hạn mức: "
          + (long) maxBid + " VNĐ");

    } catch (NumberFormatException e) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Ngân sách tối đa phải là một con số.");
    } catch (Exception e) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Lỗi hệ thống: " + e.getMessage());
    }
  }
}