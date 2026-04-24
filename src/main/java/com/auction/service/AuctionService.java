package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.Item;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService implements Serializable {
    // phong ngua th doi ten bien,java se tinh ra id moi co the k khop voi id file
    // cu dc serialize len
    private static final long serialVersionUID = 1L;
    // Dùng ConcurrentHashMap để đảm bảo an toàn khi nhiều ClientHandler cùng truy
    // cập
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    // Singleton pattern,Instance (Sử dụng từ khóa volatile để an toàn đa luồng)
    private static volatile AuctionService instance;

    private AuctionService() {// khoi tao du lieu mau/
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) {
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        if (auction != null) {
            auctions.put(auction.getId(), auction);
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

    // Hỗ trọ Serialization
    // Sau khi load từ file, ta cần gán lại instance để cả hệ thống dùng chung bản
    // vừa load
    public static void setInstance(AuctionService loadedInstance) {
        // chúng ta đang ở trong một hàm static (hàm của lớp, không phải của đối tượng
        // cụ thể). Lúc này, chưa có đối tượng instance nào tồn tại để mà khóa, nên ta
        // phải dùng chính cái "khuôn mẫu" của lớp đó (AuctionService.class) làm điểm
        // tựa để khóa.
        synchronized (AuctionService.class) {
            instance = loadedInstance;
        }
    }

}
