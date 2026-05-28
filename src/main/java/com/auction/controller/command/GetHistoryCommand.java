package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.Auction;
import com.auction.model.entities.BidTransaction;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;

/**
 * lay lich su trong phien dau luc dat gia.
 */
public class GetHistoryCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Kiểm tra an toàn: Đảm bảo payload có ít nhất 2 phần (Lệnh | Mã Auction)
    if (!client.validatePayload(parts, 2)) {
      return;
    }

    String auctionId = parts[1];
    Auction auction = auctionService.getAuctionById(auctionId);

    if (auction != null) {
      try {
        String jsonHistory;
        synchronized (auction) {
          // Tạo một bản sao danh sách tại thời điểm hiện tại để Gson xử lý an toàn
          java.util.List<BidTransaction> historyCopy = new java.util.ArrayList<>(auction.getBidHistory());
          jsonHistory = client.gson.toJson(historyCopy);
        }

        // Dùng client.sendMessage thay vì sendMessage
        client.sendMessage(Protocol.RES_HISTORY + Protocol.SEPARATOR
            + auctionId + Protocol.SEPARATOR + jsonHistory);
      } catch (Exception e) {
        client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
            + "Lỗi xử lý dữ liệu lịch sử: " + e.getMessage());
      }
    } else {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Không tìm thấy phiên đấu giá với ID: " + auctionId);
    }
  }
}
