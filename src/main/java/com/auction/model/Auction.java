package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;
    private Item item;
    private List<BidTransaction> history;
    private AuctionStatus status;
    private double currentPrice;

    // transient: Những trường này sẽ không được lưu xuống file .dat
    private transient ReentrantLock lock;
    private transient List<Observer> observers;

    public Auction(String id, Item item) {
        super(id);
        this.item = item;
        this.lock = new ReentrantLock();
        this.observers = new ArrayList<>();
        this.currentPrice = item.getStartingPrice();
        this.history = new ArrayList<>();
        this.status = AuctionStatus.OPEN; // Mới tạo thì để OPEN
    }

    /**
     * FIX LỖI: Sau khi deserialize, các trường transient bị null.
     * Cần gọi hàm này trong DataManager hoặc readObject.
     */
    public void restoreTransients() {
        if (this.lock == null)
            this.lock = new ReentrantLock();
        if (this.observers == null)
            this.observers = new ArrayList<>();
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
            validateAuctionStatus();
            validateBidAmount(bidAmount);

            updateAuctionState(bidder, bidAmount);
        } finally {
            lock.unlock();
        }
    }

    private void validateAuctionStatus() throws AuctionClosedException {
        // Nếu trạng thái là FINISHED, PAID hoặc CANCELED thì không cho BID nữa
        if (this.status == AuctionStatus.FINISHED ||
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
}
