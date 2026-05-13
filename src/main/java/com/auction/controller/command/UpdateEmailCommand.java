package com.auction.controller.command;

import com.auction.model.entities.user.User;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.usermanger.UserManager;
import com.auction.controller.network.ClientHandler;

public class UpdateEmailCommand implements ClientCommand {
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    if (!client.validatePayload(parts, 2)) {
      return;
    }
    User user = client.getCurrentUser();
    if (user == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Vui lòng đăng nhập trước.");
      return;
    }
    String newEmail = parts[1];
    if (UserManager.getInstance().updateEmail(user.getUsername(), newEmail)) {
      client.sendMessage(Protocol.RES_SUCCESS + Protocol.SEPARATOR + "Cập nhật thành công email");
    }else{
      client.sendMessage(Protocol.ERROR+Protocol.SEPARATOR+ "Email đã được đăng ký");
    }
  }
}
