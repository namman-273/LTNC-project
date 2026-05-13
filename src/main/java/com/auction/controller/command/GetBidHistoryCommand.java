package com.auction.controller.command;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.model.entities.user.User;
import com.auction.service.AuctionService;
import com.auction.util.core.BidHistoryManager;
import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.google.gson.Gson;
import java.util.List;

public class GetBidHistoryCommand implements ClientCommand {
  private static final Gson gson = new Gson();

  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Kiểm tra đăng nhập
    User user = client.getCurrentUser();
    if (user == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Vui lòng đăng nhập để xem lịch sử.");
      return;
    }

    // Lấy danh sách lịch sử từ Manager (Manager này đã được DataManager load từ
    // file .dat lên RAM)
    List<BidHistoryEntry> history = BidHistoryManager.getInstance()
        .getHistoryForUser(user.getUsername());

    // 3. Chuyển thành JSON Array
    // Kết quả sẽ có dạng:
    // [{"auctionId":"...","itemName":"...","result":"WIN",...},...]
    String jsonHistory = gson.toJson(history);

    // 4. Gửi về FE
    // Format: BID_HISTORY_RES|[{"id":1...},{"id":2...}]
    client.sendMessage(Protocol.RES_BID_HISTORY + Protocol.SEPARATOR + jsonHistory);
  }
}
