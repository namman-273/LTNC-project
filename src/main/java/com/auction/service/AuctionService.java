package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.CreateItem;
import com.auction.model.Item;
import com.auction.model.Observer;
import com.auction.model.User;
import com.auction.util.DataManager;
import com.auction.factory.ItemFactory;

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

    public synchronized void createNewAuction(String itemType, String itemName, double startingPrice,
            long durationMinutes) {
        // 1. Tạo ID duy nhất cho phiên đấu giá (Ví dụ: AUC_171400...)
        String auctionId = "AUC_" + System.currentTimeMillis();

        // 2. Sử dụng Factory để tạo Item
        ItemFactory factory = CreateItem.getFactory(itemType);
        Item newItem = factory.create(auctionId, itemName, startingPrice);

        // 3. Khởi tạo đối tượng Auction mới
        Auction newAuction = new Auction(auctionId, newItem, durationMinutes);

        // 4. Lưu vào bộ nhớ (HashMap/List trong Service)
        this.auctions.put(auctionId, newAuction);

        // 5. Lưu xuống file .dat ngay lập tức
        DataManager.getInstance().saveData();

        // 6. Thông báo cho tất cả người dùng đang kết nối về sản phẩm mới
        // (Optional: Nếu bạn có hệ thống Global Observer)
    }

    /**
     * FIX LỖI: Singleton bị phá khi deserialize
     * Java sẽ gọi hàm này sau khi load file để đảm bảo chỉ có 1 instance duy nhất.
     */
    // Sửa lại hàm readResolve để không làm mất dữ liệu đã load
    protected Object readResolve() {
        // Khi load từ file, gán instance hiện tại chính là đối tượng vừa load
        instance = this;

        // Khởi tạo lại scheduler vì nó là transient (không được lưu xuống file)
        if (this.scheduler == null || this.scheduler.isShutdown()) {
            this.scheduler = Executors.newScheduledThreadPool(5);
        }

        return instance;
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

    // Dùng để lấy toàn bộ dữ liệu Map auctions để lưu xuống file
    public Map<String, Auction> getAuctionsMap() {
        return this.auctions; // auctions là cái Map<String, Auction>
    }

    // Dùng để khôi phục dữ liệu sau khi đọc từ file .dat lên
    public void setAuctions(Map<String, Auction> loadedAuctions) {
        if (loadedAuctions != null) {
            this.auctions.clear(); // Xóa sạch dữ liệu trắng hiện tại
            this.auctions.putAll(loadedAuctions); // Đổ toàn bộ dữ liệu từ file vào
        }
    }

    public void shutdown() {
        System.out.println("[SERVICE] Đang tiến hành dọn dẹp và lưu dữ liệu...");

        // Dừng ScheduledExecutorService để không tạo thêm thread mới
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // Đợi tối đa 5 giây cho các tác vụ đang chạy hoàn tất
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        for (Auction auction : auctions.values()) {
            auction.shutdownNotifier();
        }

        // QUAN TRỌNG: Lưu toàn bộ dữ liệu hiện tại xuống file .dat
        // Điều này đảm bảo giá thầu và trạng thái phiên đấu giá được bảo toàn
        try {
            DataManager.getInstance().saveData();
            System.out.println("[SERVICE] Dữ liệu đã được lưu an toàn vào file .dat.");
        } catch (Exception e) {
            System.err.println("[SERVICE ERROR] Không thể lưu dữ liệu khi shutdown: " + e.getMessage());
        }
    }

    // Trong AuctionService.java
    public void removeObserverFromAll(Observer obs) {
        for (Auction auction : auctions.values()) {
            auction.removeObserver(obs);
        }
    }
}
