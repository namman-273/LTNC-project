package com.auction.model;

import java.util.HashSet;
import java.util.Set;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    // Tính năng Watchlist: Lưu các auctionId người dùng đang theo dõi
    private Set<String> watchlist = new HashSet<>();

    public Bidder(String username, String password) {
        super(username, password, "BIDDER");
    }

    @Override
    public void displayInfo() {
        System.out.println("[com.auction.model.Bidder] ID: " + id + ", Name: " + username);
    }

    public String getName() {
        return username;
    }
    // --- LOGIC WATCHLIST ---

    public void addToWatchlist(String auctionId) {
        if (auctionId != null && !auctionId.isEmpty()) {
            watchlist.add(auctionId);
        }
    }

    public void removeFromWatchlist(String auctionId) {
        watchlist.remove(auctionId);
    }

    public Set<String> getWatchlist() {
        // Trả về một bản sao để bảo vệ tính đóng gói của dữ liệu gốc
        return new HashSet<>(watchlist);
    }
}
