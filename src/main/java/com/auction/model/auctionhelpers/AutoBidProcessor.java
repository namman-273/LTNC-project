package com.auction.model.auctionhelpers;

import com.auction.model.entities.Auction;
import com.auction.model.entities.AutoBid;
import com.auction.model.entities.BidTransaction;
import com.auction.model.entities.user.User;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.InvalidBidException;
import java.util.PriorityQueue;

/**
 * AutoBidProcessor - Phiên bản tối ưu hóa tuyệt đối cho hệ thống Auction
 * Concurrency.
 * Tích hợp Shadow History, xử lý Tie-Breaker trùng MaxBid và đồng bộ hóa qua
 * Factory.
 */
public class AutoBidProcessor {

  @FunctionalInterface
  public interface BidUpdater {
    void updateState(User user, double amount) throws InvalidBidException;
  }

  public void executeAutoBids(PriorityQueue<AutoBid> queue, Auction auction, BidUpdater updater) {
    if (queue == null || queue.isEmpty()) {
      return;
    }

    // 1. GIẢI QUYẾT TRUY CẬP: Lấy Validator thông qua Factory
    AuctionValidator validator = AuctionHelperFactory.getInstance().createValidator();

    double currentPrice = auction.getCurrentPrice();
    double minIncrement = validator.getMinimumIncrement(currentPrice);

    // 2. THANH LỌC TẬN GỐC: Xóa sạch các Bot rác đã hết ngân sách ở mọi ngóc ngách
    queue.removeIf(bot -> (currentPrice + minIncrement > bot.getMaxBid()));

    if (queue.isEmpty()) {
      return;
    }

    // Xác định Người đang dẫn đầu hiện tại từ lịch sử phiên
    String lastBidderId = auction.getBidHistory().isEmpty() ? ""
        : auction.getBidHistory().get(auction.getBidHistory().size() - 1)
            .getBidder().getId(); // Trực tiếp dùng getId() từ Entity kế thừa

    // Lấy Winner tiềm năng nhất ra ( đứng đầu Heap thỏa mãn MaxBid cao nhất +
    // Đăng ký trước)
    AutoBid top = queue.poll();
    double finalPrice;
    AutoBid second = null;

    // 3. THUẬT TOÁN PROXY BIDDING & TIE-BREAKER
    if (queue.isEmpty()) {
      // Tình huống A: Chỉ còn duy nhất 1 Bot cấu hình hợp lệ
      if (top.getBidderId().equals(lastBidderId)) {
        queue.add(top); // Trả lại Queue bảo toàn dữ liệu
        return; // Bot này đang giữ top rồi, không tự nâng giá của mình nữa
      }
      double effectiveStep = Math.max(top.getbidStep(), minIncrement);
      finalPrice = currentPrice + effectiveStep;
    } else {
      // Tình huống B: Có từ 2 Bot trở lên tranh chấp giằng co
      second = queue.peek(); // Chỉ xem trước (peek) hứ không poll Bot thứ hai ra khỏi Heap
      double effectiveStep = Math.max(top.getbidStep(), minIncrement);

      if (top.getMaxBid() == second.getMaxBid()) {
        // LUẬT TIE-BREAKER: 2 Bot trùng giá trần -> 'top' ăn nhờ lợi thế thời
        // gian (timestamp nhỏ hơn)
        // Đẩy giá lên thẳng mức Trần tối đa của đối thủ để loại bỏ cuộc chơi ngay lập
        // tức
        finalPrice = top.getMaxBid();
      } else {
        // Luật Đấu giá giá thứ hai (Vickrey Auction) thông thường
        double targetPrice = second.getMaxBid() + effectiveStep;
        finalPrice = Math.min(targetPrice, top.getMaxBid());
      }

      // Đảm bảo bước nhảy tối thiểu so với lượt Manual Bid vừa gọi lệnh này
      if (finalPrice <= currentPrice) {
        finalPrice = currentPrice + effectiveStep;
      }
    }

    // Hàng rào bảo hiểm cuối cùng bảo vệ MaxBid của Winner
    if (finalPrice > top.getMaxBid()) {
      finalPrice = top.getMaxBid();
    }

    // Tìm kiếm User thật từ Database/RAM thông qua Service
    User winnerUser = UserManager.getInstance().findUserByUsername(top.getBidderId());
    if (winnerUser == null) {
      queue.add(top); // Trả lại cấu hình nếu không tìm thấy User
      return;
    }

    if (second != null && finalPrice > (currentPrice + minIncrement)) {
      double tempPrice = currentPrice;
      // Xác định lượt nổ súng: Thằng nào không giữ vị trí dẫn đầu sẽ chủ động nâng
      // giá trước
      AutoBid currentTurnBot = top.getBidderId().equals(lastBidderId) ? second : top;

      while (true) {
        double loopMinInc = validator.getMinimumIncrement(tempPrice);
        double loopStep = Math.max(currentTurnBot.getbidStep(), loopMinInc);
        tempPrice += loopStep;

        if (tempPrice >= finalPrice) {
          break;
        }

        // Chỉ add vào loop nếu chưa đến finalPrice
        if (tempPrice <= currentTurnBot.getMaxBid()) {
          User intermediateUser = UserManager.getInstance()
              .findUserByUsername(currentTurnBot.getBidderId());
          if (intermediateUser != null) {
            auction.getBidHistory().add(new BidTransaction(intermediateUser, tempPrice));
          }
        }

        // Đổi lượt luân phiên (Ping-pong) giữa 2 Bot hàng đầu
        currentTurnBot = (currentTurnBot == top) ? second : top;
      }

    }

    try {
      // Hàm này thực thi updateState của Auction -> gọi FinancialProcessor trừ tiền
      // thật,
      updater.updateState(winnerUser, finalPrice);

      // Đánh giá xem Winner có còn đủ tiền để chiến đấu tiếp ở các lượt đặt tay sau
      // hay không
      double nextMinBid = finalPrice + validator.getMinimumIncrement(finalPrice);
      if (nextMinBid <= top.getMaxBid()) {
        queue.add(top); // Đẩy lại Winner vào Heap để bảo vệ vị trí ở các lượt gọi sau
      } else {
        System.out.println("[AUTOBID] Winner " + top.getBidderId() + " đã cạn cấu hình ngân sách.");
      }
    } catch (InvalidBidException e) {
      System.err.println("[AUTOBID CRITICAL ERROR] " + e.getMessage());
      queue.add(top); // Trả lại hàng đợi đề phòng lỗi hệ thống logic tài chính ngoài ý muốn
    }
  }
}