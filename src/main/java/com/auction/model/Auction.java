package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.PriorityQueue;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;
    private Item item;
    private List<BidTransaction> history;
    private AuctionStatus status;
    private double currentPrice;
    private long endTime; // Thời điểm kết thúc (ms)
    private PriorityQueue<AutoBid> autoBidQueue; // Hàng đợi ưu tiên

    // transient: Những trường này sẽ không được lưu xuống file .dat
    private transient ReentrantLock lock;
    private transient List<Observer> observers;

    public Auction(String id, Item item, long durationMinutes) {
        super(id);
        this.item = item;
        this.lock = new ReentrantLock();
        this.observers = new ArrayList<>();
        this.currentPrice = item.getStartingPrice();
        this.history = new ArrayList<>();
        this.status = AuctionStatus.OPEN; // Mới tạo thì để OPEN
        this.autoBidQueue = new PriorityQueue<>();
        this.endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
    }

    /**
     * FIX LỖI: Sau khi deserialize, các trường transient bị null.
     * Cần gọi hàm này trong DataManager hoặc readObject.
     */
    public void restoreTransients() {
        // BẮT BUỘC: Vì ReentrantLock không thể lưu xuống file
        if (this.lock == null)
            this.lock = new ReentrantLock();

        // BẮT BUỘC: Vì các kết nối Observer/Socket phải đăng ký lại từ đầu khi Client
        // kết nối
        if (this.observers == null)
            this.observers = new ArrayList<>();

        // Để tránh lỗi nếu load file .dat từ phiên bản code cũ chưa có Auto-bid
        if (this.autoBidQueue == null)
            this.autoBidQueue = new PriorityQueue<>();
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

    // --- LOGIC QUẢN LÝ OBSERVER (Public để Service gọi được) ---

    public void addObserver(Observer obs) {
        if (observers == null)
            restoreTransients();
        observers.add(obs);
    }

    public void removeObserver(Observer obs) {
        if (observers != null)
            observers.remove(obs);
    }

    public void notifyObservers(String msg) {
        if (observers == null)
            return;
        for (Observer obs : observers) {
            obs.update(msg);
        }
    }

    // --- LOGIC PHIÊN ĐẤU GIÁ ---

    public void processNewBid(User bidder, double bidAmount)
            throws InvalidBidException, AuctionClosedException, AuthenticationException {
        lock.lock();
        try {
            validateAuthentication(bidder);
            validateAuctionStatus();// Cần check thêm cả thời gian endTime
            validateBidAmount(bidAmount);

            updateAuctionState(bidder, bidAmount);
            long timeLeft = this.endTime - System.currentTimeMillis();
            if (timeLeft > 0 && timeLeft < 60000) { // < 60s
                this.endTime += 90000; // Gia hạn thêm 90s
                notifyObservers("SNIPING|" + getId() + "|" + this.endTime);
            }

            // Kích hoạt hệ thống tự động trả giá
            executeAutoBids();
        } finally {
            lock.unlock();
        }
    }

    private void validateAuctionStatus() throws AuctionClosedException {
        // Nếu trạng thái là FINISHED, PAID hoặc CANCELED hoặc hết giờ thì không cho BID
        // nữa
        long currentTime = System.currentTimeMillis();
        if (currentTime > endTime ||
                this.status == AuctionStatus.FINISHED ||
                this.status == AuctionStatus.PAID ||
                this.status == AuctionStatus.CANCELED) {
            throw new AuctionClosedException("Phiên đấu giá không còn trong thời gian đặt giá.");
        }
    }

    private void validateBidAmount(double amount) throws InvalidBidException {
        if (amount <= currentPrice) {
            throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: " + currentPrice);
        }
    }

    private void validateAuthentication(User bidder) throws AuthenticationException {
        if (bidder == null) {
            throw new AuthenticationException("Người dùng chưa đăng nhập!");
        }
    }

    private void updateAuctionState(User bidder, double amount) {
        this.currentPrice = amount;
        if (this.item != null) {
            this.item.setCurrentPrice(amount);
        }
        // Lưu lịch sử giao dịch
        this.history.add(new BidTransaction(bidder, amount));

        notifyObservers("UPDATE|" + getId() + "|" + amount + "|" + bidder.getUsername());
    }

    // Hàm để người dùng đăng ký Auto-bid từ giao diện
    public void addAutoBidConfig(String bidderId, double maxBid, double increment) {
        lock.lock();
        try {
            if (this.autoBidQueue == null)
                restoreTransients();
            this.autoBidQueue.add(new AutoBid(bidderId, maxBid, increment));

            // Thử chạy ngay xem có outbid được giá hiện tại không
            executeAutoBids();
        } finally {
            lock.unlock();
        }
    }

    // Định nghĩa logic máy tự động trả giá [MỤC NÂNG CAO]
    private void executeAutoBids() {
        if (autoBidQueue == null || autoBidQueue.isEmpty())
            return;

        // Lấy người có maxBid cao nhất ra xem xét
        AutoBid top = autoBidQueue.peek();

        // Lấy ID người vừa đặt giá cao nhất hiện tại
        String lastBidderId = history.isEmpty() ? "" : history.get(history.size() - 1).getBidder().getUsername();

        // ĐIỀU KIỆN:
        // - Không tự outbid chính mình
        // - Giá mới (current + increment) không vượt quá giới hạn (maxBid)
        double nextAutoPrice = currentPrice + top.getIncrement();

        if (!top.getBidderId().equals(lastBidderId) && nextAutoPrice <= top.getMaxBid()) {

            // Giả lập một User đại diện cho người đặt Auto-bid
            User autoUser = new User(top.getBidderId(), "SYSTEM_AUTO", "CLIENT") {
                // Để trống vì ta chỉ cần các hàm có sẵn của User/Entity
            };

            // Cập nhật trạng thái (Sử dụng hàm update để notify luôn)
            updateAuctionState(autoUser, nextAutoPrice);

            // Đệ quy: Sau khi máy bid, kiểm tra xem có cấu hình Auto-bid nào khác cao hơn
            // nữa không
            executeAutoBids();
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject(); // Load các trường không phải transient
        restoreTransients(); // Tự động hồi sinh các trường bị null
    }
}
