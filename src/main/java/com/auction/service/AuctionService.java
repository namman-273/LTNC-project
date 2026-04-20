package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.Item;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    // Dùng ConcurrentHashMap để đảm bảo an toàn khi nhiều ClientHandler cùng truy cập
    private  final Map<String, Auction> auctions=new ConcurrentHashMap<>();
    public AuctionService(){//khoi tao du lieu mau/
    }
    public void addAuction(Auction auction){
        if (auction!=null){
            auctions.put(auction.getId(),auction);
        }
    }
    public Auction getAuctionById(String id) {
        return auctions.get(id);
    }
    /**
     * Lấy toàn bộ danh sách phiên đấu giá.
     * FE sẽ cần cái này để hiển thị danh mục hàng hóa.
     */
    public Collection<Auction> getAllAuctions() {
        return auctions.values();
    }
    public Item getItemInAuction(String auctionId) {
        Auction a = auctions.get(auctionId);
        return (a != null) ? a.getItem() : null;
    }

}
