package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.network.ClientHandler;
import com.auction.network.Protocol;
import com.auction.service.UserManager;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private static final long ONE_MINUTE_MS = 600000L;
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

  /**
   * .
   */
  public Auction(String id, Item item, long durationMinutes, String sellerId) {
    super(id);
    if (item == null) {
      throw new IllegalArgumentException("Item cannot be null "
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
   * Cần gọi hàm này trong DataManager hoặc readObject.
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

  }

  // --- CÁC GETTER/SETTER QUAN TRỌNG ---

  public AuctionStatus getStatus() {
    return status;
  }

  /**
   * .
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

  // --- LOGIC QUẢN LÝ OBSERVER (Public để Service gọi được) ---

  /**
   * Thêm vào cho observer theo dõi.
   */
  public void addObserver(Observer obs) {
    if (observers == null) {
      restoreTransients();
      if (!observers.contains(obs)) {
        observers.add(obs);
      }
    }
    observers.add(obs);
  }

  /**
   * xóa khỏi theo dõi.
   */
  public void removeObserver(Observer obs) {
    if (observers != null) {
      observers.remove(obs);
    }
  }

  // --- NEW NOTIFICATION METHODS ---

  /**
   * IMPROVED: Get the current highest bidder (last person to bid).
   * 
   * <p>User who placed the last bid, or null if no bids yet
   */
  private User getPreviousHighestBidder() {
    if (history.isEmpty()) {
      return null;
    }
    return history.get(history.size() - 1).getBidder();
  }

  /**
   * . Chỉ duyệt qua danh sách observers (ClientHandler)
   * vì đây mới là những đường ống Socket có thể gửi tin nhắn về máy khách.
   */
  public void notifyAllParticipants(String message, User excludeUser) {
    if (observers == null || observers.isEmpty()) {
      return;
    }

    for (Observer observer : observers) {
      // Ép kiểu sang ClientHandler để lấy thông tin User đang giữ kết nối này
      if (observer instanceof ClientHandler) {
        ClientHandler handler = (ClientHandler) observer;

        // Bỏ qua không gửi cho người vừa tạo ra hành động này (để tránh tự spam chính
        // mình)
        if (excludeUser != null && handler.getCurrentUser() != null) {
          if (handler.getCurrentUser().getUsername().equals(excludeUser.getUsername())) {
            continue;
          }
        }
      }

      // Gửi tin nhắn bất đồng bộ
      notifyExecutor.submit(() -> {
        try {
          if (observer != null) {
            observer.update(message);
          }
        } catch (Exception e) {
          removeObserver(observer); // Nếu Socket sập, gỡ luôn khỏi danh sách
          System.out.println("Removed faulty observer: " + e.getMessage());
        }
      });
    }

    System.out.println("[NOTIFICATION] Đã phát sóng thông báo tới "
        + observers.size() + " đường truyền mạng.");
  }

  /**
   * NEW: Gắn thẳng tin nhắn vào Socket của một người dùng cụ thể.
   * Dùng để gửi NOTI_OUTBID (Bị vượt giá).
   */

  public void notifySpecificUser(String targetUsername, String message) {
    if (observers == null
        || observers.isEmpty()
        || targetUsername == null) {
      return;
    }

    for (Observer observer : observers) {
      if (observer instanceof ClientHandler) {
        ClientHandler handler = (ClientHandler) observer;

        if (handler.getCurrentUser() != null
            && handler.getCurrentUser().getUsername().equals(targetUsername)) {
          notifyExecutor.submit(() -> {
            try {
              observer.update(message);
            } catch (Exception e) {
              removeObserver(observer);
            }
          });
          break; // Tìm thấy và gửi rồi thì dừng vòng lặp
        }
      }
    }
  }

  // --- LOGIC PHIÊN ĐẤU GIÁ ---
  private double getMinimumIncrement(double price) {
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

  /**
   * Đấu giá.
   */
  public void processNewBid(User bidder, double bidAmount)
      throws InvalidBidException, AuctionClosedException, AuthenticationException {
    lock.lock();
    try {
      validateAuthentication(bidder);
      validateAuctionStatus(); // Cần check thêm cả thời gian endTime
      validateBidAmount(bidAmount);

      updateAuctionState(bidder, bidAmount);
      handleAntiSniping(bidder);

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
      // Việt hóa thông báo lỗi với đơn vị VNĐ
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
    User previousHighestBidder = getPreviousHighestBidder();
    // Refund previous bidder (only after new bidder's money is secured)
    if (previousHighestBidder != null && !previousHighestBidder.equals(bidder)) {
      BidTransaction lastTransaction = history.get(history.size() - 1);
      double refundAmount = lastTransaction.getAmount();
      previousHighestBidder.addBalance(refundAmount);

      // IMPROVED: Send specific "OUTBID" notification to previous highest bidder
      String outbidMessage = Protocol.NOTI_OUTBID + Protocol.SEPARATOR
          + getId() + Protocol.SEPARATOR
          + bidder.getUsername() + Protocol.SEPARATOR
          + amount;
      notifySpecificUser(previousHighestBidder.getUsername(), outbidMessage);
      // 2. Gửi thông báo hoàn tiền (REFUND) - SỬ DỤNG LỆNH CÓ SẴN
      // Cấu trúc gợi ý: NOTI_REFUND|AuctionID|Số_tiền_được_hoàn|Số_dư_hiện_tại
      String refundMessage = Protocol.NOTI_REFUND + Protocol.SEPARATOR
          + getId() + Protocol.SEPARATOR
          + refundAmount + Protocol.SEPARATOR
          + previousHighestBidder.getBalance();
      notifySpecificUser(previousHighestBidder.getUsername(), refundMessage);

    }

    this.currentPrice = amount;
    if (this.item != null) {
      this.item.setCurrentPrice(amount);
    }
    // Lưu lịch sử giao dịch
    this.history.add(new BidTransaction(bidder, amount));

    String bidUpdateMessage = Protocol.NOTI_BID_UPDATE + Protocol.SEPARATOR
        + getId() + "|" + amount + "|" + bidder.getUsername();
    notifyAllParticipants(bidUpdateMessage, bidder);

  }

  /**
   * // Hàm để người dùng đăng ký Auto-bid từ giao diện.
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

      // Xóa cấu hình cũ của người này nếu có (để cập nhật cấu hình mới)
      autoBidQueue.removeIf(config -> config.getBidderId().equals(bidderId));

      this.autoBidQueue.add(new AutoBid(bidderId, maxBid, customStep));
      System.out.println("SERVER: Đã nhận cấu hình Auto-bid cho " + bidderId);

      executeAutoBids();
    } finally {
      lock.unlock();
    }
  }

  // Định nghĩa logic máy tự động trả giá [MỤC NÂNG CAO]
  private void executeAutoBids() {
    if (autoBidQueue == null || autoBidQueue.isEmpty()) {
      return;
    }

    int maxIterations = 100; // Chống treo Server và đệ quy vô hạn
    int count = 0;

    while (!autoBidQueue.isEmpty() && count < maxIterations) {
      count++;

      // 1. Lấy bot tiếp theo ra khỏi hàng đợi
      AutoBid top = autoBidQueue.poll();

      String lastBidderId = history.isEmpty() ? ""
          : history.get(history.size() - 1).getBidder().getUsername();

      // 2. Nếu ng mạnh nhất đang thắng, lấy ngay ng mạnh thứ hai ra đấu
      if (top.getBidderId().equals(lastBidderId)) {
        AutoBid second = autoBidQueue.poll();
        if (second == null) {
          autoBidQueue.add(top);
          break; // Hết đối thủ
        }
        // Trả ng mạnh nhất vào lại để đợi đối thủ nâng giá
        autoBidQueue.add(top);
        top = second; // Đổi mục tiêu sang ng thứ hai
      }

      // 3. Tính toán mức giá mới
      double nextPrice = currentPrice + top.getbidStep();

      // 4. Kiểm tra ngân sách tối đa của bot (Max Bid)
      if (nextPrice <= top.getMaxBid()) {
        User user = UserManager.getInstance().findUserByUsername(top.getBidderId());
        if (user != null) {
          try {
            // Gọi hàm Atomic Swap (Trừ trước - Hoàn sau) để an toàn ví tiền
            updateAuctionState(user, nextPrice);

            // Đấu giá thành công, đưa bot trở lại hàng đợi cho lượt sau
            autoBidQueue.add(top);
          } catch (InvalidBidException e) {
            System.out.println("bot dừng do lỗi: " + e.getMessage());
            // Nếu lỗi do hết số dư ví (Sổ dư), không add lại vào Queue để tránh loop lỗi
            if (!e.getMessage().contains("Số dư")) {
              autoBidQueue.add(top);
            }
          }
        }
      } else {
        // TRƯỜNG HỢP DỪNG: Ngân sách MaxBid đã chạm giới hạn
        // bot sẽ bị loại khỏi Queue (không được add lại)
        System.out.println("bot của " + top.getBidderId() + " đã chạm giới hạn ngân sách.");
      }
    }
  }

  private void handleAntiSniping(User bidder) {
    long timeLeft = this.endTime - System.currentTimeMillis();
    if (timeLeft > 0 && timeLeft < ONE_MINUTE_MS && extensionCount < MAX_EXTENSIONS) { // < 1 phút
      this.endTime += TWO_MINUTES_MS; // Cộng thêm 2 phút
      this.extensionCount++;

      String message = Protocol.NOTI_SNIPING_UPDATE
          + Protocol.SEPARATOR + getId()
          + "|" + this.endTime
          + "|" + extensionCount;

      // Truyền null vào excludeUser vì ngay cả người vừa bid cũng cần thấy EndTime
      // mới trên UI của họ
      notifyAllParticipants(message, null);
      System.out.println("[ANTI-SNIPING] Phiên "
          + getId() + " được gia hạn thêm 2p bởi " + bidder.getUsername());
    }
  }

  private void readObject(java.io.ObjectInputStream in)
      throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject(); // Load các trường không phải transient
    restoreTransients(); // Tự động hồi sinh các trường bị null
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

  /**
   * giải phóng tài nguyên khi phiên đấu giá kết thúc hoặc Server dừng .
   */
  public void closeAuction() {
    this.status = AuctionStatus.FINISHED;

    // Xóa danh sách người theo dõi để giải phóng bộ nhớ
    if (observers != null) {
      observers.clear();
    }

    // Xóa hàng chờ Auto-bid vì phiên đã đóng
    if (autoBidQueue != null) {
      autoBidQueue.clear();
    }
    if (notifyExecutor != null && !notifyExecutor.isShutdown()) {
      notifyExecutor.shutdown(); // Giải phóng threads ngay lập tức
    }
  }

  public String getSellerId() {
    return this.sellerId;
  }

}
