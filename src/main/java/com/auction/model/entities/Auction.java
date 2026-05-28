package com.auction.model.entities;

import com.auction.model.auctionhelpers.AuctionFinancialProcessor;
import com.auction.model.auctionhelpers.AuctionHelperFactory;
import com.auction.model.auctionhelpers.AuctionNotifier;
import com.auction.model.auctionhelpers.AuctionSnipingProcessor;
import com.auction.model.auctionhelpers.AuctionValidator;
import com.auction.model.auctionhelpers.AutoBidProcessor;
import com.auction.model.entities.item.Item;
import com.auction.model.entities.user.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.observer.Observer;
import com.auction.util.exception.AuctionClosedException;
import com.auction.util.exception.AuthenticationException;
import com.auction.util.exception.InvalidBidException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an auction entity for managing item bidding with concurrent access
 * support.
 * Handles auction lifecycle, auto-bidding, and observer notifications.
 */
public class Auction extends Entity {
  private static final long serialVersionUID = 1L;

  private static final int THREAD_POOL_SIZE = 10;
  private static final long TWO_MINUTES_MS = 120000L; // ep kieu sang long
  private static final long ONE_MINUTE_MS = 60000L;
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

  // CÁC HELPER ĐƯỢC THÊM VÀO (transient)
  private transient AuctionValidator validator;
  private transient AuctionNotifier notifier;
  private transient AutoBidProcessor autoBidProcessor;
  private transient AuctionFinancialProcessor financialProcessor;
  private transient AuctionSnipingProcessor snipingProcessor;

  /**
   * Constructor.
   */
  public Auction(String id, Item item, long durationMinutes, String sellerId) {
    super(id);
    if (item == null) {
      throw new IllegalArgumentException("Item cannot be null"
          + ".Mỗi phiên đấu giá phải có một món hàng!");
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
   * Cần gọi hàm này trong DataManager.
   */
  public void restoreTransients() {
    this.extensionCount = 0;
    // BẮT BUỘC: Vì ReentrantLock không thể lưu xuống file
    if (this.lock == null) {
      this.lock = new ReentrantLock();
    }
    // BẮT BUỘC: Vì các kết nối Observer/Socket phải đăng ký lại từ đầu khi Client
    // kết nối
    if (this.observers == null) {
      this.observers = new ArrayList<>();
    }
    // Để tránh lỗi nếu load file .dat từ phiên bản code cũ chưa có Auto-bid
    if (this.autoBidQueue == null) {
      this.autoBidQueue = new PriorityQueue<>();
    }
    if (this.notifyExecutor == null || this.notifyExecutor.isShutdown()) {
      this.notifyExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    AuctionHelperFactory factory = AuctionHelperFactory.getInstance();
    if (this.validator == null) {
      this.validator = factory.createValidator();
    }
    if (this.notifier == null) {
      this.notifier = factory.createNotifier();
    }
    if (this.autoBidProcessor == null) {
      this.autoBidProcessor = factory.createAutoBidProcessor();
    }
    if (this.financialProcessor == null) {
      this.financialProcessor = factory.createFinancialProcessor();
    }
    if (this.snipingProcessor == null) {
      this.snipingProcessor = factory.createSnipingProcessor();
    }
  }

  // --- CÁC GETTER/SETTER QUAN TRỌNG ---
  public AuctionStatus getStatus() {
    return status;
  }

  /**
   * Chỉnh trạng thái phiên.
   */
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

  public String getSellerId() {
    return this.sellerId;
  }

  public String getId() {
    return super.getId();
  }

  @Override
  public String toString() {
    return "id=" + getId() + ",itemName=" + (item != null ? item.getItemName() : "---")
        + ",currentPrice=" + currentPrice + ",status=" + status;
  }

  // --- LOGIC QUẢN LÝ OBSERVER (Public để Service gọi được) ---

  /**
   * Thêm observer.
   */
  public void addObserver(Observer obs) {
    if (observers == null) {
      restoreTransients();
    }
    if (!observers.contains(obs)) {
      observers.add(obs);
    }
  }

  /**
   * Xóa observer.
   */
  public void removeObserver(Observer obs) {
    if (observers != null) {
      observers.remove(obs);
    }
  }

  // --- NEW NOTIFICATION METHODS ---
  private User getPreviousHighestBidder() {
    if (history.isEmpty()) {
      return null;
    }
    return history.get(history.size() - 1).getBidder();
  }

  /**
   * Thông báo nhờ observer cho chung.
   */
  public void notifyAllParticipants(String message, User excludeUser) {
    if (notifier == null) {
      restoreTransients();
    }
    notifier.notifyAllParticipants(this, this.observers, this.notifyExecutor, message, excludeUser);
  }

  /**
   * Thông báo riêng.
   */
  public void notifySpecificUser(String targetUsername, String message) {
    if (notifier == null) {
      restoreTransients();
    }
    notifier.notifySpecificUser(this, this.observers, this.notifyExecutor, targetUsername, message);
  }

  // --- LOGIC PHIÊN ĐẤU GIÁ ---
  /**
   * lấy giá sàn.
   */
  public double getMinimumIncrement(double price) {
    if (validator == null) {
      restoreTransients();
    }
    return validator.getMinimumIncrement(price);
  }

  /**
   * Xử lý bid mới.
   */
  public void processNewBid(User bidder, double bidAmount)
      throws InvalidBidException, AuctionClosedException, AuthenticationException {
    lock.lock();
    try {
      validateAuthentication(bidder);
      validateAuctionStatus();
      validateBidAmount(bidAmount);

      updateAuctionState(bidder, bidAmount);
      handleAntiSniping(bidder);

      executeAutoBids();
    } finally {
      lock.unlock();
    }
  }

  private void validateAuctionStatus() throws AuctionClosedException {
    if (validator == null) {
      restoreTransients();
    }
    validator.validateAuctionStatus(this.status, this.endTime);
  }

  private void validateBidAmount(double amount) throws InvalidBidException {
    if (validator == null) {
      restoreTransients();
    }
    validator.validateBidAmount(this.currentPrice, amount);
  }

  private void validateAuthentication(User bidder) throws AuthenticationException {
    if (validator == null) {
      restoreTransients();
    }
    validator.validateAuthentication(bidder);
  }

  private void updateAuctionState(User bidder, double amount) throws InvalidBidException {
    if (financialProcessor == null) {
      restoreTransients();
    }
    financialProcessor.processTransaction(this, bidder, amount, getPreviousHighestBidder(),
        (newPrice, newTransaction) -> {
          this.currentPrice = newPrice;
          if (this.item != null) {
            this.item.setCurrentPrice(newPrice);
          }
          this.history.add(newTransaction);
        });
  }

  /**
   * Đăng ký autobid.
   */
  public void addAutoBidConfig(String bidderId, double maxBid, double customStep)
      throws InvalidBidException {
    lock.lock();
    try {
      double systemMin = getMinimumIncrement(currentPrice);
      if (customStep < systemMin) {
        throw new InvalidBidException("Bước giá tự động phải lớn hơn hoặc bằng "
            + (long) systemMin + " VNĐ");
      }
      if (this.autoBidQueue == null) {
        restoreTransients();
      }

      autoBidQueue.removeIf(config -> config.getBidderId().equals(bidderId));
      this.autoBidQueue.add(new AutoBid(bidderId, maxBid, customStep));
      System.out.println("SERVER: Đã nhận cấu hình Auto-bid cho " + bidderId);

      executeAutoBids();
    } finally {
      lock.unlock();
    }
  }

  
private void executeAutoBids() {
    if (autoBidProcessor == null) {
        restoreTransients();
    }
    autoBidProcessor.executeAutoBids(this.autoBidQueue,
        this, (user, price) -> this.updateAuctionState(user, price));

    broadcastFullHistory();
}

private void broadcastFullHistory() {
    if (observers == null || observers.isEmpty()) return;

    // Dùng Gson để serialize — cần import com.google.gson.Gson
    com.google.gson.Gson gson = new com.google.gson.Gson();
    String jsonHistory = gson.toJson(this.history);
    String msg = com.auction.network.protocol.Protocol.RES_HISTORY
            + com.auction.network.protocol.Protocol.SEPARATOR
            + this.getId()
            + com.auction.network.protocol.Protocol.SEPARATOR
            + jsonHistory;

    for (Observer obs : observers) {
        notifyExecutor.submit(() -> {
            try {
                obs.update(msg);
            } catch (Exception e) {
                removeObserver(obs);
            }
        });
    }
}



  private void handleAntiSniping(User bidder) {
    if (snipingProcessor == null) {
      restoreTransients();
    }
    snipingProcessor.handleAntiSniping(this, bidder,
        ONE_MINUTE_MS, TWO_MINUTES_MS, MAX_EXTENSIONS, this.extensionCount,
        (extraTime) -> {
          this.endTime += extraTime;
          this.extensionCount++;
        });
  }

  /**
   * Giải phóng tài nguyên khi đóng phiên.
   * Status đã được set bởi AuctionEndHandler:
   * - FINISHED: Admin đóng sớm hoặc không có winner
   * - PAID: Kết thúc bình thường và thanh toán thành công
   * 
   */
  public void closeAuction() {
    // Phải chờ notifyExecutor drain hết task trước khi clear
    // observers, vì notifyAllParticipants() submit task async vào executor.
    // Nếu clear observers trước thì task notify chạy nhưng observers rỗng -> mất
    // thông báo.
    if (notifyExecutor != null && !notifyExecutor.isShutdown()) {
      notifyExecutor.shutdown();
      try {
        notifyExecutor.awaitTermination(3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    // Sau khi executor drain xong mới clear observers
    if (observers != null) {
      observers.clear();
    }
    if (autoBidQueue != null) {
      autoBidQueue.clear();
    }
    System.out.println("[AUCTION] Đã giải phóng tài nguyên cho phiên: " + getId());
  }
}