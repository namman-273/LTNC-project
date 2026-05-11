package com.auction.controller.command;

import com.auction.controller.network.ClientHandler;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;

public class CreateAuctionCommand implements ClientCommand {
  @Override
  public void execute(String[] parts, ClientHandler client, AuctionService auctionService) {
    // Rào chắn bảo vệ: Cần ít nhất 5 phần (Lệnh | Loại | Tên SP | Giá khởi điểm |
    // Thời gian)
    if (!client.validatePayload(parts, 5))
      return;

    // Kiểm tra trạng thái đăng nhập
    if (client.getCurrentUser() == null) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Bạn phải đăng nhập.");
      return;
    }

    // Kiểm tra phân quyền: Chỉ ADMIN hoặc SELLER mới được tạo
    String role = client.getCurrentUser().getRole();
    if (!"ADMIN".equals(role) && !"SELLER".equals(role)) {
      client.sendMessage(
          Protocol.ERROR + Protocol.SEPARATOR + "Quyền hạn không đủ. Chỉ Seller hoặc Admin mới được đăng bán.");
      return;
    }

    try {
      String type = parts[1];
      String name = parts[2];
      double price = Double.parseDouble(parts[3]);
      long duration = Long.parseLong(parts[4]);

      // Xử lý tạo mới bằng AuctionService
      auctionService.createNewAuction(type, name, price, duration, client.getCurrentUser().getUsername());

      // Báo thành công
      client.sendMessage(Protocol.RES_SUCCESS + Protocol.SEPARATOR
          + "Sản phẩm " + name + " đã được đăng sàn.");

    } catch (NumberFormatException e) {
      // Tách riêng lỗi NumberFormat để thông báo rõ ràng hơn
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá tiền hoặc thời lượng phải là một con số hợp lệ.");
    } catch (Exception e) {
      client.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Dữ liệu tạo sản phẩm không hợp lệ: " + e.getMessage());
    }
  }
}