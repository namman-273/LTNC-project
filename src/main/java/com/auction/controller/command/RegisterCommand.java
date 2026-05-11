package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;

public class RegisterCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    if (!client.validatePayload(parts, 4))
      return; // 4 là REQ_REGISTER

    boolean success = UserManager.getInstance().register(parts[1], parts[2], parts[3]);
    if (success) {
      client.sendMessage(Protocol.RES_REGISTER_SUCCESS + Protocol.SEPARATOR + "Đăng ký thành công.");
    } else {
      client.sendMessage(Protocol.RES_REGISTER_FAILED + Protocol.SEPARATOR + "Tên người dùng đã tồn tại.");
    }
  }
}
