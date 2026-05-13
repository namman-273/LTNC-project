package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.user.User;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.AuthenticationException;

public class LoginCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Cần ít nhất 3 phần: Lệnh | Username | Password
    if (!client.validatePayload(parts, 3))
      return;

    try {
      // Xác thực User
      User user = UserManager.getInstance().login(parts[1], parts[2]);

      if (user != null) {
        // Gán User vào ClientHandler hiện tại
        client.setCurrentUser(user);

        // Báo login thành công
        client.sendMessage(Protocol.RES_LOGIN_SUCCESS + Protocol.SEPARATOR + user.getRole()
            + Protocol.SEPARATOR + "Chào " + user.getUsername());
      }
    } catch (AuthenticationException e) {
      // Báo lỗi sai mật khẩu hoặc tài khoản
      client.sendMessage(Protocol.RES_LOGIN_FAILED + Protocol.SEPARATOR + e.getMessage());
    }
  }
}