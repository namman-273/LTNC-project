package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp đại diện cho một giao dịch đặt giá.
 * Đã tách riêng để FE dễ dàng parse lịch sử đấu giá.
 */
public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private User bidder;
    private double amount;
    private LocalDateTime timestamp;

    public BidTransaction(User bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    // --- GETTERS & SETTERS (Bắt buộc để FE đọc được dữ liệu) ---

    public User getBidder() {
        return bidder;
    }

    public void setBidder(User bidder) {
        this.bidder = bidder;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Hỗ trợ in lịch sử đẹp mắt trên Console hoặc gửi về FE.
     */
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return String.format("[%s] %s bid: %.2f",
                timestamp.format(formatter),
                bidder.getUsername(),
                amount);
    }
}