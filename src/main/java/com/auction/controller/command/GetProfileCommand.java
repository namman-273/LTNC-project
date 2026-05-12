package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.user.User;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;

public class GetProfileCommand implements ClientCommand {
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    User user = client.getCurrentUser();
    if (user == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Bạn chưa đăng nhập.");
      return;
    }

    // Format: PROFILE_INFO|username|email|role|balance|joinDate
    String email = (user.getEmail() != null) ? user.getEmail() : "@";
    double balance = user.getBalance();
    String joinDate = user.getJoinDate();
    if (joinDate == null || joinDate.equals("N/A")) {
      joinDate = "01/01/2026"; // Gán một ngày mặc định cho các user "cựu chiến binh"
    }

    String response = String.format("%s|%s|%s|%s|%.2f|%s",
        Protocol.RES_PROFILE_INFO,
        user.getUsername(),
        email,
        user.getRole(),
        balance,
        joinDate);

    client.sendMessage(response);
  }
}
