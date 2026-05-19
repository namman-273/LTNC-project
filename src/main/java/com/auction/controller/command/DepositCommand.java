package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.util.core.DataManager;

/**
 * nap tien.
 */
public class DepositCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Rào chắn bảo vệ: Cần ít nhất 2 phần (Lệnh | Số tiền)
    if (!client.validatePayload(parts, 2)) {
      return;
    }

    if (client.getCurrentUser() == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Vui lòng đăng nhập để nạp tiền.");
      return;
    }

    try {
      double amount = Double.parseDouble(parts[1]);

      // Validate số tiền an toàn (lớn hơn 0, không phải NaN, không phải vô cực)
      if (amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount)) {

        // Cộng tiền cho User
        client.getCurrentUser().addBalance(amount);

        // Lưu dữ liệu ngay lập tức xuống file
        DataManager.getInstance().saveData();

        // Báo cáo thành công
        client.sendMessage(Protocol.RES_DEPOSIT_SUCCESS + Protocol.SEPARATOR
            + client.getCurrentUser().getBalance() + Protocol.SEPARATOR
            + "Đã nạp thành công: " + amount);
      } else {
        client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Số tiền nạp không hợp lệ.");
      }
    } catch (NumberFormatException e) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá nạp phải là con số.");
    }
  }
}