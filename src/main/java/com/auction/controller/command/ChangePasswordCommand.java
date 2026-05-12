package com.auction.controller.command;

import com.auction.model.entities.user.User;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;
import com.auction.controller.network.ClientHandler;

public class ChangePasswordCommand implements ClientCommand {
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    if (!client.validatePayload(parts, 3))
      return;
    User user = client.getCurrentUser();
    if (user == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Vui lòng đăng nhập trước.");
      return;
    }
    String oldPass = parts[1];
    String newPass = parts[2];
    if (UserManager.getInstance().updatePassword(user.getUsername(), oldPass, newPass)) {
      client.sendMessage(Protocol.RES_SUCCESS + Protocol.SEPARATOR
          + "Cập nhật mật khẩu mới thành công!");
    } else {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR
          + "Cần nhập đúng mật khẩu cũ và mật khẩu mới không được giống mật khẩu cũ.");
    }
  }

}
