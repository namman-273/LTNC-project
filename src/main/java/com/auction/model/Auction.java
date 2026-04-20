package com.auction.model;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
class BidTransaction{
    private Bidder bidder;
    private double amount;
    private long timestamp;
    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }
}
public class Auction extends Entity {
    private Item item;
    private List<BidTransaction> history;
    private AuctionStatus status;
    private double currentPrice;
    private final ReentrantLock lock = new ReentrantLock();
    private List<Observer> observers=new ArrayList<>();

    public Auction(String id, Item item) {
        super(id);
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.history = new ArrayList<>();
        this.status= AuctionStatus.OPEN;
    }
    public Item getItem() {
        return this.item;
    }
    public double getCurrentPrice() {
        if (this.item != null) {
            return this.item.getCurrentPrice();
        }
        return 0.0;
    }

    public void addObserver(Observer obs) { observers.add(obs); }
    public void removeObserver(Observer obs) {
        lock.lock();
        try {
            if (observers.contains(obs)) {
                observers.remove(obs);
                System.out.println("DEBUG: Đã gỡ 1 Observer khỏi phiên " + getId());
            }
        } finally {
            lock.unlock();
        }
    }
    private void notifyObservers(String msg) {
        for (Observer obs : observers) obs.update(msg);
    }
    //Khi này là bat dau vao phien chay
    public void startAuction() {
        lock.lock();
        try {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
                notifyObservers("Phiên đấu giá " + getId() + " đã bắt đầu !");
            }
        } finally { lock.unlock(); }
    }

    public void endAuction() {
        lock.lock();
        try {
            status = AuctionStatus.FINISHED;
            notifyObservers("Phiên đấu giá " + getId() + " đã kết thúc !");
        } finally { lock.unlock(); }
    }
    public void processNewBid(Bidder bidder, double bidAmount) throws InvalidBidException, AuctionClosedException, AuthenticationException {
        lock.lock();
        try {
            //dam bao ocp
            validateAuthentication(bidder);
            validateAuctionStatus();
            validateBidAmount(bidAmount);

            updateAuctionState(bidder, bidAmount);
        } finally {
            // Phải luôn luôn mở khóa trong khối finally
            // đảm bảo  có lỗi xảy ra, khóa vẫn  giải phóng cho người sau
            lock.unlock();
        }
    }
    private void validateAuctionStatus() throws AuctionClosedException {
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Trạng thái " + status + " không cho phép đặt giá.");
        }
    }
    private void validateBidAmount(double amount) throws InvalidBidException {
        if (amount <= currentPrice) {
            throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: "+ this.item.getCurrentPrice());
        }
    }
    private void validateAuthentication(Bidder bidder) throws AuthenticationException {
    //  Kiểm tra đăng nhập (người dùng tồn tại)
    if (bidder == null) {
        throw new AuthenticationException("Lỗi: Người dùng không tồn tại hoặc chưa đăng nhập.");
    }
}

    private void updateAuctionState(Bidder bidder, double amount) {
        this.item.setCurrentPrice(amount);
        this.history.add(new BidTransaction(bidder, amount));
        notifyObservers("UPDATE|" + this.item.getId() + "|" + amount + "|" + bidder.getUsername());
    }
}
