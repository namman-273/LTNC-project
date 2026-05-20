package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.util.core.DataManager;

public class WatchCommand implements ClientCommand {
    @Override
    public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
        // Kiểm tra payload để đảm bảo không bị IndexOutOfBoundsException khi lấy
        // parts[1]
        if (!client.validatePayload(parts, 2))
            return;

        // Kiểm tra role của User
        if (!(client.getCurrentUser() instanceof Bidder)) {
            client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
                    + "Chỉ người mua mới có thể theo dõi sản phẩm.");
            return;
        }

        String auctionId = parts[1];

        // Thêm vào watchlist của user
        boolean isSuccess = ((Bidder) client.getCurrentUser()).addToWatchlist(auctionId);

        if (isSuccess) {
            DataManager.getInstance().saveData();
            client.sendMessage(Protocol.RES_WATCH_SUCCESS + Protocol.SEPARATOR + auctionId);

            Auction targetAuction = auctionService.getAuctionById(auctionId);
            if (targetAuction != null) {
                // Đăng ký client (ClientHandler) để nhận tin nhắn
                targetAuction.addObserver(client);
                System.out.println("[WATCHLIST] User " + client.getCurrentUser().getUsername()
                        + " đã bắt đầu nhận thông báo từ phiên " + auctionId);
            }
        } else {
            // Thêm phản hồi nếu Watchlist đầy hoặc sản phẩm đã có sẵn
            client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
                    + "Theo dõi thất bại! (Sản phẩm đã có trong danh sách hoặc không tồn tại)");
        }
    }
}