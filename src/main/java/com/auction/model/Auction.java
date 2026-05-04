package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.PriorityQueue;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    private static final int THREAD_POOL_SIZE = 10;
    private static final long ONE_MINUTE_MS = 60000L; // ép kiểu sang Long
    private static final long TWO_MINUTES_MS = 120000L;
    private static final int MAX_EXTENSIONS = 3;

    private Item item;
    private List<BidTransaction> history;
    private AuctionStatus status;
    private double currentPrice;
    private long endTime; // Thời điểm kết thúc (ms)
    private PriorityQueue<AutoBid> autoBidQueue; // Hàng đợi ưu tiên
    private String sellerId; // ID người tạo phiên đấu giá

    // transient: Những trường này sẽ không được lưu xuống file .dat
    private transient ReentrantLock lock;
    private transient List<Observer> observers;
    private transient ExecutorService notifyExecutor;
    private transient int extensionCount = 0;

    public Auction(String id, Item item, long durationMinutes, String sellerId) {
        super(id);
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null. Mỗi phiên đấu giá phải có một món hàng!");
        } else {
            this.item = item;
        }
        this.sellerId = sellerId;
        this.currentPrice = item.getStartingPrice();
        this.history = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.autoBidQueue = new PriorityQueue<>();
        this.endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
        restoreTransients();
    }

    /**
     * FIX LỖI: Sau khi deserialize, các trường transient bị null.
     * Cần gọi hàm này trong DataManager hoặc readObject.
     */
    public void restoreTransients() {
        this.extensionCount = 0;
        // FIX [NeedBraces]: thêm {} cho if một dòng
        if (this.lock == null) {
            this.lock = new ReentrantLock();
        }
        if (this.observers == null) {
            this.observers = new ArrayList<>();
        }
        if (this.autoBidQueue == null) {
            this.autoBidQueue = new PriorityQueue<>();
        }
        if (this.notifyExecutor == null || this.notifyExecutor.isShutdown()) {
            this.notifyExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        }
    }

    // --- CÁC GETTER/SETTER QUAN TRỌNG ---

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.lock.lock();
        try {
            this.status = status;
        } finally {
            this.lock.unlock();
        }
    }

    public List<BidTransaction> getBidHistory() {
        return this.history;
    }

    public Item getItem() {
        return this.item;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public long getEndTime() {
        return endTime;
    }

    // --- LOGIC QUẢN LÝ OBSERVER (Public để Service gọi được) ---

    public void addObserver(Observer obs) {
        // FIX [NeedBraces]: thêm {} cho if một dòng
        if (observers == null) {
            restoreTransients();
        }
        observers.add(obs);
    }

    public void removeObserver(Observer obs) {
        // FIX [NeedBraces]: thêm {} cho if một dòng
        if (observers != null) {
            observers.remove(obs);
        }
    }

    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            notifyExecutor.submit(() -> {
                try {
                    if (observer != null) {
                        observer.update(message);
                    }
                } catch (Exception e) {
                    removeObserver(observer);
                    System.out.println("Removed faulty observer: " + e.getMessage());
                }
            });
        }
    }

    // --- LOGIC PHIÊN ĐẤU GIÁ ---
    private double getMinimumIncrement(double price) {
        // FIX [NeedBraces]: thêm {} cho if một dòng
        if (price < 1000000) {
            return 50000; // < 1 triệu: bước 50k
        }
        if (price < 5000000) {
            return 100000; // < 5 triệu: bước 100k
        }
        if (price < 10000000) {
            return 250000; // < 10 triệu: bước 250k
        }
        return 500000; // >= 10 triệu: bước 500k
    }

    public void processNewBid(User bidder, double bidAmount)
            throws InvalidBidException, AuctionClosedException, AuthenticationException {
        lock.lock();
        try {
            validateAuthentication(bidder);
            validateAuctionStatus(); // Cần check thêm cả thời gian endTime
            validateBidAmount(bidAmount);

            updateAuctionState(bidder, bidAmount);
            handleAntiSniping();

            // Kích hoạt hệ thống tự động trả giá
            executeAutoBids();
        } finally {
            lock.unlock();
        }
    }

    private void validateAuctionStatus() throws AuctionClosedException {
        long currentTime = System.currentTimeMillis();
        if (currentTime > endTime
                || this.status == AuctionStatus.FINISHED
                || this.status == AuctionStatus.PAID
                || this.status == AuctionStatus.CANCELED) {
            throw new AuctionClosedException("Phiên đấu giá không còn trong thời gian đặt giá.");
        }
    }

    private void validateBidAmount(double amount) throws InvalidBidException {
        double minInc = getMinimumIncrement(currentPrice);
        double minRequired = currentPrice + minInc;

        if (amount < minRequired) {
            throw new InvalidBidException("Giá đặt không hợp lệ. Bạn cần đặt tối thiểu: "
                    + (long) minRequired + " VNĐ (Bước giá tối thiểu: " + (long) minInc + " VNĐ)");
        }
    }

    private void validateAuthentication(User bidder) throws AuthenticationException {
        if (bidder == null) {
            throw new AuthenticationException("Người dùng chưa đăng nhập!");
        }
    }

    private void updateAuctionState(User bidder, double amount) throws InvalidBidException {
        // 1. CHỐT CHẶN BẢO MẬT: Người bán không được tự đấu giá
        if (bidder.getUsername().equals(this.sellerId)) {
            throw new InvalidBidException("Bạn không thể đấu giá sản phẩm của chính mình!");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidBidException("Giá đặt không hợp lệ (NaN/Infinite)");
        }
        // TRỪ TIỀN TẠM GIỮ CỦA NGƯỜI MỚI
        if (!bidder.deductBalance(amount)) {
            throw new InvalidBidException("Số dư tài khoản không đủ để đặt mức giá này!");
        }

        // HOÀN TIỀN CHO NGƯỜI CŨ (Chỉ khi đã cầm được tiền của người mới)
        if (!history.isEmpty()) {
            BidTransaction lastTransaction = history.get(history.size() - 1);
            User oldBidder = lastTransaction.getBidder();
            if (oldBidder != null && !oldBidder.equals(bidder)) {
                oldBidder.addBalance(lastTransaction.getAmount());
                oldBidder.update("REFUND|Phiên " + getId() + " bị vượt giá. Đã hoàn: "
                        + lastTransaction.getAmount());
            }
        }

        this.currentPrice = amount;
        if (this.item != null) {
            this.item.setCurrentPrice(amount);
        }
        // Lưu lịch sử giao dịch
        this.history.add(new BidTransaction(bidder, amount));

        notifyObservers("UPDATE|" + getId() + "|" + amount + "|" + bidder.getUsername());
    }

    // Hàm để người dùng đăng ký Auto-bid từ giao diện
    public void addAutoBidConfig(String bidderId, double maxBid) {
        lock.lock();
        try {
            // FIX [NeedBraces]: thêm {} cho if một dòng
            if (this.autoBidQueue == null) {
                restoreTransients();
            }
            autoBidQueue.removeIf(config -> config.getBidderId().equals(bidderId));
            this.autoBidQueue.add(new AutoBid(bidderId, maxBid));
            System.out.println("SERVER: Đã nhận cấu hình Auto-bid cho " + bidderId);
            executeAutoBids();
        } finally {
            lock.unlock();
        }
    }

    // Định nghĩa logic máy tự động trả giá [MỤC NÂNG CAO]
    private void executeAutoBids() {
        // FIX [NeedBraces]: thêm {} cho if một dòng
        if (autoBidQueue == null || autoBidQueue.isEmpty()) {
            return;
        }

        int maxIterations = 100; // Chống treo Server và đệ quy vô hạn
        int count = 0;

        while (!autoBidQueue.isEmpty() && count < maxIterations) {
            count++;

            AutoBid top = autoBidQueue.poll();
            String lastBidderId = history.isEmpty()
                    ? ""
                    : history.get(history.size() - 1).getBidder().getUsername();

            if (top.getBidderId().equals(lastBidderId)) {
                autoBidQueue.add(top);
                break;
            }

            double systemMinIncrement = getMinimumIncrement(currentPrice);
            double nextPrice = currentPrice + systemMinIncrement;

            if (nextPrice <= top.getMaxBid()) {
                User user = UserManager.getInstance().findUserByUsername(top.getBidderId());
                if (user != null) {
                    try {
                        updateAuctionState(user, nextPrice);
                        autoBidQueue.add(top);
                    } catch (InvalidBidException e) {
                        System.out.println("bot dừng do lỗi: " + e.getMessage());
                        if (!e.getMessage().contains("Số dư")) {
                            autoBidQueue.add(top);
                        }
                    }
                }
            } else {
                System.out.println("Robot của " + top.getBidderId() + " đã chạm giới hạn ngân sách.");
            }
        }
    }

    private synchronized void handleAntiSniping() {
        long timeLeft = this.endTime - System.currentTimeMillis();
        if (timeLeft > 0 && timeLeft < ONE_MINUTE_MS && extensionCount < MAX_EXTENSIONS) {
            this.endTime += TWO_MINUTES_MS; // Cộng thêm 2 phút
            this.extensionCount++;
            notifyObservers("SNIPING|" + getId() + "|" + this.endTime + "|" + extensionCount);
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        restoreTransients();
    }

    public String getId() {
        return super.getId();
    }

    @Override
    public String toString() {
        return "id=" + getId()
                + ",itemName=" + (item != null ? item.getItemName() : "---")
                + ",currentPrice=" + currentPrice
                + ",status=" + status;
    }

    // giải phóng tài nguyên khi phiên đấu giá kết thúc hoặc Server dừng
    public void closeAuction() {
        this.status = AuctionStatus.FINISHED;

        if (observers != null) {
            observers.clear();
        }
        if (autoBidQueue != null) {
            autoBidQueue.clear();
        }
        if (notifyExecutor != null && !notifyExecutor.isShutdown()) {
            notifyExecutor.shutdown();
        }
    }

    public String getSellerId() {
        return this.sellerId;
    }
}
