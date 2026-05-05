package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an auction session, managing bids, observers, and auto-bidding.
 */
public class Auction extends Entity {

  private static final long serialVersionUID = 1L;

  private static final int THREAD_POOL_SIZE = 10;

  /** Duration of one minute in milliseconds. */
  private static final long ONE_MINUTE_MS = 60_000L;

  /** Extension duration added when anti-sniping triggers. */
  private static final long TWO_MINUTES_MS = 120_000L;

  private static final int MAX_EXTENSIONS = 3;

  /** Maximum auto-bid loop iterations to prevent infinite loops. */
  private static final int MAX_AUTO_BID_ITERATIONS = 100;

  private Item item;

  private List<BidTransaction> history;

  private AuctionStatus status;

  private double currentPrice;

  /** Auction end timestamp in milliseconds. */
  private long endTime;

  private PriorityQueue<AutoBid> autoBidQueue;

  /** Username of the seller who created this auction. */
  private String sellerId;

  // transient: these fields are not serialized to .dat files
  private transient ReentrantLock lock;

  private transient List<Observer> observers;

  private transient ExecutorService notifyExecutor;

  private transient int extensionCount = 0;

  /**
   * Constructs a new Auction.
   *
   * @param id              unique identifier
   * @param item            the item being auctioned
   * @param durationMinutes auction duration in minutes
   * @param sellerId        username of the seller
   */
  public Auction(String id, Item item, long durationMinutes, String sellerId) {
    super(id);
    if (item == null) {
      throw new IllegalArgumentException(
          "Item cannot be null. Mỗi phiên đấu giá phải có một món hàng!");
    }
    this.item = item;
    this.sellerId = sellerId;
    this.currentPrice = item.getStartingPrice();
    this.history = new ArrayList<>();
    this.status = AuctionStatus.OPEN;
    this.autoBidQueue = new PriorityQueue<>();
    this.endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
    restoreTransients();
  }

  /**
   * Restores transient fields after deserialization.
   * Must be called by DataManager or via readObject.
   */
  public void restoreTransients() {
    this.extensionCount = 0;
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

  // --- GETTERS / SETTERS ---

  /**
   * Returns the current auction status.
   *
   * @return the status
   */
  public AuctionStatus getStatus() {
    return status;
  }

  /**
   * Sets the auction status in a thread-safe manner.
   *
   * @param status the new status
   */
  public void setStatus(AuctionStatus status) {
    this.lock.lock();
    try {
      this.status = status;
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns the full bid history.
   *
   * @return list of bid transactions
   */
  public List<BidTransaction> getBidHistory() {
    return this.history;
  }

  /**
   * Returns the item being auctioned.
   *
   * @return the item
   */
  public Item getItem() {
    return this.item;
  }

  /**
   * Returns the current highest bid price.
   *
   * @return the current price
   */
  public double getCurrentPrice() {
    return currentPrice;
  }

  /**
   * Returns the auction end time as a Unix timestamp in milliseconds.
   *
   * @return end time in ms
   */
  public long getEndTime() {
    return endTime;
  }

  // --- OBSERVER MANAGEMENT ---

  /**
   * Registers an observer to receive auction events.
   *
   * @param obs the observer to add
   */
  public void addObserver(Observer obs) {
    if (observers == null) {
      restoreTransients();
    }
    observers.add(obs);
  }

  /**
   * Removes a previously registered observer.
   *
   * @param obs the observer to remove
   */
  public void removeObserver(Observer obs) {
    if (observers != null) {
      observers.remove(obs);
    }
  }

  /**
   * Notifies all registered observers with the given message.
   *
   * @param message the event message
   */
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

  // --- AUCTION LOGIC ---

  private double getMinimumIncrement(double price) {
    if (price < 1_000_000) {
      return 50_000;
    }
    if (price < 5_000_000) {
      return 100_000;
    }
    if (price < 10_000_000) {
      return 250_000;
    }
    return 500_000;
  }

  /**
   * Processes a new bid from the given user.
   *
   * @param bidder    the user placing the bid
   * @param bidAmount the bid amount
   * @throws InvalidBidException      if the bid is invalid
   * @throws AuctionClosedException   if the auction is no longer open
   * @throws AuthenticationException  if the user is not authenticated
   */
  public void processNewBid(User bidder, double bidAmount)
      throws InvalidBidException, AuctionClosedException, AuthenticationException {
    lock.lock();
    try {
      validateAuthentication(bidder);
      validateAuctionStatus();
      validateBidAmount(bidAmount);
      updateAuctionState(bidder, bidAmount);
      handleAntiSniping();
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
      throw new InvalidBidException(
          "Giá đặt không hợp lệ. Bạn cần đặt tối thiểu: "
              + (long) minRequired + " VNĐ (Bước giá tối thiểu: " + (long) minInc + " VNĐ)");
    }
  }

  private void validateAuthentication(User bidder) throws AuthenticationException {
    if (bidder == null) {
      throw new AuthenticationException("Người dùng chưa đăng nhập!");
    }
  }

  private void updateAuctionState(User bidder, double amount) throws InvalidBidException {
    if (bidder.getUsername().equals(this.sellerId)) {
      throw new InvalidBidException("Bạn không thể đấu giá sản phẩm của chính mình!");
    }
    if (Double.isNaN(amount) || Double.isInfinite(amount)) {
      throw new InvalidBidException("Giá đặt không hợp lệ (NaN/Infinite)");
    }
    if (!bidder.deductBalance(amount)) {
      throw new InvalidBidException("Số dư tài khoản không đủ để đặt mức giá này!");
    }
    if (!history.isEmpty()) {
      BidTransaction lastTransaction = history.get(history.size() - 1);
      User oldBidder = lastTransaction.getBidder();
      if (oldBidder != null && !oldBidder.equals(bidder)) {
        oldBidder.addBalance(lastTransaction.getAmount());
        oldBidder.update(
            "REFUND|Phiên " + getId() + " bị vượt giá. Đã hoàn: "
                + lastTransaction.getAmount());
      }
    }
    this.currentPrice = amount;
    if (this.item != null) {
      this.item.setCurrentPrice(amount);
    }
    this.history.add(new BidTransaction(bidder, amount));
    notifyObservers("UPDATE|" + getId() + "|" + amount + "|" + bidder.getUsername());
  }

  /**
   * Registers an auto-bid configuration for the given bidder.
   *
   * @param bidderId the bidder's username
   * @param maxBid   the maximum bid the auto-bidder will place
   */
  public void addAutoBidConfig(String bidderId, double maxBid) {
    lock.lock();
    try {
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

  private void executeAutoBids() {
    if (autoBidQueue == null || autoBidQueue.isEmpty()) {
      return;
    }
    int count = 0;
    while (!autoBidQueue.isEmpty() && count < MAX_AUTO_BID_ITERATIONS) {
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
      this.endTime += TWO_MINUTES_MS;
      this.extensionCount++;
      notifyObservers("SNIPING|" + getId() + "|" + this.endTime + "|" + extensionCount);
    }
  }

  private void readObject(java.io.ObjectInputStream in)
      throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject();
    restoreTransients();
  }

  /**
   * Returns the unique auction ID.
   *
   * @return the ID
   */
  @Override
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

  /**
   * Closes the auction, clears observers, and releases thread-pool resources.
   */
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

  /**
   * Returns the seller's username.
   *
   * @return the seller ID
   */
  public String getSellerId() {
    return this.sellerId;
  }
}
