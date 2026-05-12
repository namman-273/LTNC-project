package com.auction.model.auctionhelpers;

import com.auction.model.entities.Auction;
import com.auction.model.entities.AutoBid;
import com.auction.model.entities.user.User;
import com.auction.service.UserManager;
import com.auction.util.exception.InvalidBidException;

import java.util.PriorityQueue;

public class AutoBidProcessor {

  @FunctionalInterface
  public interface BidUpdater {
    void updateState(User user, double amount) throws InvalidBidException;
  }

  public void executeAutoBids(PriorityQueue<AutoBid> queue, Auction auction, BidUpdater updater) {
    if (queue == null || queue.isEmpty())
      return;

    int maxIterations = 100; // Chống treo Server và đệ quy vô hạn
    int count = 0;

    while (!queue.isEmpty() && count < maxIterations) {
      count++;
      // 1. Lấy bot tiếp theo ra khỏi hàng đợi
      AutoBid top = queue.poll();

      String lastBidderId = auction.getBidHistory().isEmpty() ? ""
          : auction.getBidHistory().get(auction.getBidHistory().size() - 1).getBidder().getUsername();

      if (top.getBidderId().equals(lastBidderId)) {
        AutoBid second = queue.poll();
        if (second == null) {
          queue.add(top);
          break; // Hết đối thủ
        }
        // Trả ng mạnh nhất vào lại để đợi đối thủ nâng giá
        queue.add(top);
        top = second; // Đổi mục tiêu sang ng thứ hai
      }

      double nextPrice = auction.getCurrentPrice() + top.getbidStep();
      // 4. Kiểm tra ngân sách tối đa của bot (Max Bid)
      if (nextPrice <= top.getMaxBid()) {
        User user = UserManager.getInstance().findUserByUsername(top.getBidderId());
        if (user != null) {
          try {
            updater.updateState(user, nextPrice);
            // Đấu giá thành công, đưa bot trở lại hàng đợi cho lượt sau
            queue.add(top);
          } catch (InvalidBidException e) {
            System.out.println("bot dừng do lỗi: " + e.getMessage());
            // Nếu lỗi do hết số dư ví (Sổ dư), không add lại vào Queue để tránh loop lỗi
            if (!e.getMessage().contains("Số dư")) {
              queue.add(top);
            }
          }
        }
      } else {
        // TRƯỜNG HỢP DỪNG: Ngân sách MaxBid đã chạm giới hạn
        // bot sẽ bị loại khỏi Queue (không được add lại)
        System.out.println("bot của " + top.getBidderId() + " đã chạm giới hạn ngân sách.");
      }
    }
  }
}
