package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.Auction;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.util.core.DataManager;

/**
 * lenh thuc ti dat gia.
 */
public class BidCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Rào chắn bảo vệ: Cần ít nhất 3 phần (Lệnh | Mã Auction | Số tiền)
    if (!client.validatePayload(parts, 3)) {
      return;
    }

    if (client.getCurrentUser() == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Bạn phải đăng nhập trước khi đấu giá!");
      return;
    }

    try {
      String auctionId = parts[1];
      double amount = Double.parseDouble(parts[2]);

      if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
        client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá tiền không hợp lệ.");
        return;
      }

      Auction auction = auctionService.getAuctionById(auctionId);
      if (auction == null) {
        client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
            + "Không tìm thấy phiên đấu giá này");
        return;
      }

      // Chuyền đối tượng User vào hàm xử lý
      auction.processNewBid(client.getCurrentUser(), amount);

      // Thay this thành client: Đăng ký nhận thông báo cho socket hiện tại
      auction.addObserver(client);

      client.sendMessage(Protocol.RES_BID_SUCCESS + Protocol.SEPARATOR + auctionId
          + Protocol.SEPARATOR + amount);

      // Lưu trạng thái mới (tiền bị trừ, lịch sử bid tăng lên)
      DataManager.getInstance().saveData();

    } catch (NumberFormatException e) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá tiền phải là con số hợp lệ");
    } catch (Exception e) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + e.getMessage());
    }
  }
}