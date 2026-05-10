package com.auction.model.entities;

import java.io.Serializable;

/**
 *  * Tự đấu giá.
 *  
 */
public class AutoBid implements Comparable<AutoBid>, Serializable {
  private static final long serialVersionUID = 1L;
  private final String bidderId;
  private final double maxBid; // Giá tối đa người dùng sẵn sàng trả
  private final long timestamp; // Thời điểm đăng ký để ưu tiên người đến trước
  private final double bidStep;

  /**
   *  * Constructor.
   *  
   */
  public AutoBid(String bidderId, double maxBid, double bidStep) {
    this.bidderId = bidderId;
    this.maxBid = maxBid;
    this.timestamp = System.currentTimeMillis();
    this.bidStep = bidStep;
  }

  @Override
  public int compareTo(AutoBid other) {
    // Ưu tiên 1: maxBid cao nhất
    if (this.maxBid != other.maxBid) {
      return Double.compare(other.maxBid, this.maxBid);
    }
    // Ưu tiên 2: Ai đăng ký trước thắng (xử lý xung đột đồng thời)
    return Long.compare(this.timestamp, other.timestamp);
  }

  // Getters...
  public String getBidderId() {
    return bidderId;
  }

  public double getMaxBid() {
    return maxBid;
  }

  public double getbidStep() {
    return bidStep;
  }

}