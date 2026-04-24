package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.Item;
import com.auction.model.User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService implements Serializable {
    private static final long serialVersionUID = 1L;

    // Yêu cầu: Sử dụng ScheduledExecutorService để tự đóng phiên
    // transient vì không cần lưu bộ đếm thời gian xuống file
    private transient ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private static volatile AuctionService instance;

    private AuctionService() {
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

    /**
     * FIX LỖI: Singleton bị phá khi deserialize
     * Java sẽ gọi hàm này sau khi load file để đảm bảo chỉ có 1 instance duy nhất.
     */
    protected Object readResolve() {
        // Sau khi deserialize, khởi tạo lại scheduler vì nó bị transient
        if (this.scheduler == null) {
            this.scheduler = Executors.newScheduledThreadPool(5);
        }
        return getInstance();
    }

    public void addAuction(Auction auction) {
        if (auction != null) {
            auctions.put(auction.getId(), auction);

            // TỰ ĐỘNG ĐÓNG PHIÊN SAU 5 PHÚT(tạm thời để 20s)
            scheduler.schedule(() -> {
                endAuction(auction.getId());
            }, 20000, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Logic kết thúc phiên và xác định Winner
     */
    public void endAuction(String auctionId) {
        Auction a = auctions.get(auctionId);
        if (a != null && (a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.OPEN)) {
            a.setStatus(AuctionStatus.FINISHED);

            // Xác định winner từ BidHistory
            List<BidTransaction> history = a.getBidHistory();
            User winner = null;
            double maxPrice = 0;

            for (BidTransaction tx : history) {
                if (tx.getAmount() > maxPrice) {
                    maxPrice = tx.getAmount();
                    winner = tx.getBidder();
                }
            }

            // Gửi thông báo kết quả cho các Observers (FE nhận qua socket)
            String msg = (winner != null)
                    ? "END_AUCTION_SUCCESS|" + auctionId + "|Winner:" + winner.getUsername() + "|Bid: " + maxPrice + "$"
                    : "END_AUCTION_SUCCESS|" + auctionId + "|No winner";

            a.notifyObservers(msg);
            System.out.println("[System] " + msg);
        }
    }

    public Auction getAuctionById(String id) {
        return auctions.get(id);
    }

    public Collection<Auction> getAllAuctions() {
        return auctions.values();
    }

    public Item getItemInAuction(String auctionId) {
        Auction a = auctions.get(auctionId);
        return (a != null) ? a.getItem() : null;
    }

    public static void setInstance(AuctionService loadedInstance) {
        synchronized (AuctionService.class) {
            instance = loadedInstance;
        }
    }
}
