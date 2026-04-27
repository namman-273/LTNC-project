package com.auction.model;

import java.io.Serializable;

public class AutoBid implements Comparable<AutoBid>, Serializable {
    private static final long serialVersionUID = 1L;
    private final String bidderId;
    private final double maxBid; // Giá tối đa người dùng sẵn sàng trả
    private final double increment; // Bước giá người dùng muốn hệ thống tự tăng
    private final long timestamp; // Thời điểm đăng ký để ưu tiên người đến trước

    public AutoBid(String bidderId, double maxBid, double increment) {
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.timestamp = System.currentTimeMillis();
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

    public double getIncrement() {
        return increment;
    }
}