package com.auction.model.helpers;

import com.auction.model.entities.user.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.util.exception.AuctionClosedException;
import com.auction.util.exception.AuthenticationException;
import com.auction.util.exception.InvalidBidException;

public class AuctionValidator {

  public double getMinimumIncrement(double price) {
    if (price < 1000000)
      return 50000; // < 1 triệu: bước 50k
    if (price < 5000000)
      return 100000; // < 5 triệu: bước 100k
    if (price < 10000000)
      return 250000; // < 10 triệu: bước 250k
    return 500000; // >= 10 triệu: bước 500k
  }

  public void validateAuctionStatus(AuctionStatus status, long endTime) throws AuctionClosedException {
    // Nếu trạng thái là FINISHED, PAID hoặc CANCELED hoặc hết giờ thì không cho BID
    // nữa
    long currentTime = System.currentTimeMillis();
    if (currentTime > endTime || status == AuctionStatus.FINISHED
        || status == AuctionStatus.PAID || status == AuctionStatus.CANCELED) {
      throw new AuctionClosedException("Phiên đấu giá không còn trong thời gian đặt giá.");
    }
  }

  public void validateBidAmount(double currentPrice, double amount) throws InvalidBidException {
    double minInc = getMinimumIncrement(currentPrice);
    double minRequired = currentPrice + minInc;
    if (amount < minRequired) {
      throw new InvalidBidException("Giá đặt không hợp lệ. Bạn cần đặt tối thiểu: "
          + (long) minRequired + " VNĐ (Bước giá tối thiểu: " + (long) minInc + " VNĐ)");
    }
  }

  public void validateAuthentication(User bidder) throws AuthenticationException {
    if (bidder == null) {
      throw new AuthenticationException("Người dùng chưa đăng nhập!");
    }
  }
}
