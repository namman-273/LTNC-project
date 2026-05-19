package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.util.core.DataManager;

/**
 * bo theo doi.
 */
public class UnwatchCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Kiểm tra độ dài payload (Cần ít nhất 2 phần: Lệnh | Mã Auction)
    if (!client.validatePayload(parts, 2)) {
      return;
    }

    // Kiểm tra quyền
    if (!(client.getCurrentUser() instanceof Bidder)) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Bạn chưa đăng nhập hoặc không phải là người mua (Bidder).");
      return;
    }

    String auctionId = parts[1];

    // Xóa khỏi danh sách theo dõi của User
    ((Bidder) client.getCurrentUser()).removeFromWatchlist(auctionId);

    // Gỡ ClientHandler khỏi danh sách nhận thông báo (Observer) của phiên đấu giá
    Auction auction = auctionService.getAuctionById(auctionId);
    if (auction != null) {
      auction.removeObserver(client);
      System.out.println("[UNWATCH] User " + client.getCurrentUser().getUsername()
          + " đã ngừng nhận thông báo từ phiên " + auctionId);
    }

    // Lưu dữ liệu và báo thành công
    DataManager.getInstance().saveData();
    client.sendMessage(Protocol.RES_UNWATCH_SUCCESS + Protocol.SEPARATOR + auctionId);
  }
}
