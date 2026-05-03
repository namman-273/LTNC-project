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

    public boolean addToWatchlist(String auctionId) {
        if (auctionId == null || auctionId.isEmpty()) {
            return false;
        }

        Set<String> currentWatchlist = getWatchlist();

        if (currentWatchlist.contains(auctionId)) {
            return false; // Đã có sẵn thì coi sai
        }

        currentWatchlist.add(auctionId);
        return true; // Thêm thành công
    }

    public void removeFromWatchlist(String auctionId) {
        getWatchlist().remove(auctionId);
    }

    public Set<String> getWatchlist() {
        if (watchlist == null) {
            watchlist = new HashSet<>(); // Chỉ khởi tạo khi thực sự cần dùng
        }
        return watchlist;
    }
}
