package com.auction.model.auctionhelpers;

import com.auction.model.entities.Auction;
import com.auction.model.entities.AutoBid;
import com.auction.model.entities.user.User;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.InvalidBidException;
import java.util.PriorityQueue;

/**
 * bot tu dong.
 */
public class AutoBidProcessor {

  /**
   * ap dung de xu li trang thai phien moi nhu dat gia thu cong.
   */
  @FunctionalInterface
  public interface BidUpdater {
    void updateState(User user, double amount) throws InvalidBidException;
  }

  /**
   * method dau tu dong.
   */
  public void executeAutoBids(PriorityQueue<AutoBid> queue, Auction auction, BidUpdater updater) {
    if (queue == null || queue.isEmpty()) {
      return;
    }

    int maxIterations = 100; // Chống treo Server và đệ quy vô hạn
    int count = 0;

    while (!queue.isEmpty() && count < maxIterations) {
      count++;
      // 1. Lấy bot tiếp theo ra khỏi hàng đợi
      AutoBid top = queue.poll();

      String lastBidderId = auction.getBidHistory().isEmpty() ? ""
          : auction.getBidHistory().get(auction.getBidHistory().size() - 1)
              .getBidder().getUsername();

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

      // 1. Lấy bước giá tối thiểu của hệ thống tại mức giá hiện tại
      double minSystemIncrement = auction.getMinimumIncrement(auction.getCurrentPrice());

      // 2. Chốt bước giá thực tế: Phải lớn hơn hoặc bằng mức tối thiểu của hệ thống
      double effectiveStep = Math.max(top.getbidStep(), minSystemIncrement);
      //3. Tính giá tiếp theo
      double nextPrice = auction.getCurrentPrice() + effectiveStep;;
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
