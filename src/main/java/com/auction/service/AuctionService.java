package com.auction.service;

import com.auction.model.entities.Auction;
import com.auction.model.entities.BidTransaction;
import com.auction.model.entities.item.Item;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.factory.ItemFactory;
import com.auction.model.factory.ItemFactoryRegistry;
import com.auction.model.observer.Observer;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.DataManager;
import com.auction.util.core.IDataStorage;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  * .
 *  
 */
public class AuctionService implements Serializable {
  private static final long serialVersionUID = 1L;

  // Yêu cầu: Sử dụng ScheduledExecutorService để tự đóng phiên
  // transient vì không cần lưu bộ đếm thời gian xuống file
  private transient ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

  private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
  private static volatile AuctionService instance;

  // ÁP DỤNG DIP: Khai báo Interface (dùng transient để không lỗi khi lưu file)
  private transient IDataStorage dataStorage;

  private AuctionService(IDataStorage dataStorage) {
    this.dataStorage = dataStorage;
  }

  /**
   *  * Áp dụng singleton.
   *  
   */
  public static AuctionService getInstance() {
    if (instance == null) {
      synchronized (AuctionService.class) {
        if (instance == null) {
          instance = new AuctionService(DataManager.getInstance());
        }
      }
    }
    return instance;
  }

  /**
   *  * get watchlist.
   *  
   */
  public List<Auction> getWatchlistForUser(String username) {
    User user = UserManager.getInstance().findUserByUsername(username);

    // Kiểm tra xem user có phải là Bidder không vì chỉ Bidder mới có Watchlist
    if (!(user instanceof Bidder)) {
      return Collections.emptyList();
    }

    Bidder bidder = (Bidder) user;
    return bidder.getWatchlist().stream()
        .map(this::getAuctionById)
        .filter(Objects::nonNull) // Loại bỏ nếu auction không tồn tại
        .collect(Collectors.toList());
  }

  /**
   *  * Tạo phiên mới.
   *  
   */
  public synchronized void createNewAuction(String itemType, String itemName, double startingPrice,
      long durationMinutes, String sellerId) {
    // 1. Tạo ID duy nhất cho phiên đấu giá (Ví dụ: AUC_171400...)
    String auctionId = "AUC_" + System.currentTimeMillis();

    // 2. Sử dụng Factory để tạo Item
    ItemFactory factory = ItemFactoryRegistry.getFactory(itemType);
    Item newItem = factory.create(auctionId, itemName, startingPrice);

    // 3. Khởi tạo đối tượng Auction mới
    Auction newAuction = new Auction(auctionId, newItem, durationMinutes, sellerId);

    // 4. Lưu vào bộ nhớ (HashMap/List trong Service)
    this.auctions.put(auctionId, newAuction);

    // 5. Lưu xuống file .dat ngay lập tức
    if (this.dataStorage != null) {
      this.dataStorage.saveData();
    }

    scheduler.schedule(() -> endAuction(auctionId), durationMinutes, TimeUnit.MINUTES);
  }

  /**
   * FIX 2: Hàm tiện ích giúp khôi phục lại toàn bộ báo thức (scheduler)
   * khi hệ thống khởi động lại.
   */
  private void recoverScheduledTasks() {
    if (this.scheduler == null || this.scheduler.isShutdown()) {
      this.scheduler = Executors.newScheduledThreadPool(5);
    }

    long now = System.currentTimeMillis();
    for (Auction a : auctions.values()) {
      if (a.getStatus() == AuctionStatus.OPEN) {
        long delay = a.getEndTime() - now;
        if (delay > 0) {
          // Nếu vẫn còn thời gian -> Lên lịch lại
          scheduler.schedule(() -> endAuction(a.getId()), delay, TimeUnit.MILLISECONDS);
        } else {
          // Nếu trong lúc Server tắt mà phiên đã hết giờ -> Đóng luôn lập tức
          endAuction(a.getId());
        }
      }
    }
    System.out.println("[SERVICE] Đã khôi phục lịch trình đóng phiên cho các đấu giá đang mở.");
  }

  /**
   * FIX LỖI: Singleton bị phá khi deserialize
   * Java sẽ gọi hàm này sau khi load file để đảm bảo chỉ có 1 instance duy nhất.
   */
  // Sửa lại hàm readResolve để không làm mất dữ liệu đã load
  protected Object readResolve() {
    // Khi load từ file, gán instance hiện tại chính là đối tượng vừa load
    instance = this;
    if (this.dataStorage == null) {
      this.dataStorage = DataManager.getInstance();
    }

    recoverScheduledTasks(); // Gọi khôi phục

    return instance;
  }

  /**
   * Logic kết thúc phiên và xác định Winner.
   */
  public void endAuction(String auctionId) {
    Auction a = auctions.get(auctionId);
    if (a == null) {
      return;
    }

    // 1. Dùng synchronized để đảm bảo chỉ có 1 thread được xử lý thanh toán
    synchronized (a) {
      // FIX 1: KIỂM TRA LẠI THỜI GIAN (Xử lý xung đột với Anti-sniping)
      long now = System.currentTimeMillis();
      if (now < a.getEndTime()) {
        // Nếu chưa thực sự hết giờ (do mới được cộng thêm 2 phút)
        // -> Hẹn giờ lại và hủy bỏ lần chạy này
        long remaining = a.getEndTime() - now;
        scheduler.schedule(() -> endAuction(auctionId), remaining, TimeUnit.MILLISECONDS);
        return;
      }
      // Kiểm tra lại trạng thái để tránh xử lý 2 lần (Double Payment)
      if (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID) {
        return;
      }

      // Tạm thời set FINISHED ngay để "khóa" các luồng khác chen vào
      a.setStatus(AuctionStatus.FINISHED);

      // Xác định winner từ BidHistory
      List<BidTransaction> history = a.getBidHistory();
      User winner = null;
      double maxPrice = 0;

      if (!history.isEmpty()) {
        BidTransaction lastBid = history.get(history.size() - 1);
        winner = lastBid.getBidder();
        maxPrice = lastBid.getAmount();
      }

      // 2. XỬ LÝ TÀI CHÍNH
      if (winner != null) {
        User seller = UserManager.getInstance().findUserByUsername(a.getSellerId());
        if (seller != null) {
          // Cộng tiền cho người bán (An toàn nhờ synchronized trong addBalance)
          seller.addBalance(maxPrice);
          // Nâng cấp trạng thái thành ĐÃ THANH TOÁN
          a.setStatus(AuctionStatus.PAID);

          // --- THÊM ĐOẠN NÀY: Bắn thông báo "Ting Ting" cho Seller ---
          // Format: NOTI_BALANCE_CHANGED|Số_dư_mới|+Số_tiền_cộng
          String sellerMsg = Protocol.NOTI_BALANCE_CHANGED + Protocol.SEPARATOR
              + seller.getBalance() + Protocol.SEPARATOR
              + "+" + maxPrice;

          a.notifySpecificUser(seller.getUsername(), sellerMsg);

        } else {
          System.err.println("[ERROR] Không tìm thấy seller: " + a.getSellerId());
        }
      }

      // 3. Gửi thông báo (FE nhận qua socket)
      String msg = (winner != null)
          ? Protocol.RES_END_SUCCESS + Protocol.SEPARATOR + auctionId
              + Protocol.SEPARATOR + "Winner:" + winner.getUsername()
              + Protocol.SEPARATOR + "Bid:" + maxPrice + "$"
          : Protocol.RES_END_SUCCESS + Protocol.SEPARATOR + auctionId
              + Protocol.SEPARATOR + "No winner";

      a.notifyAllParticipants(msg, null);

      // Giải phóng tài nguyên/dừng thread nếu cần
      a.closeAuction();

      // 4. LƯU DỮ LIỆU NGAY LẬP TỨC
      if (this.dataStorage != null) {
        this.dataStorage.saveData();
      }

      System.out.println("[FINANCIAL SYSTEM] Phiên " + auctionId + " hoàn tất. Trạng thái cuối: "
          + a.getStatus());
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

  /**
   *  * set instance.
   *  
   */
  public static void setInstance(AuctionService loadedInstance) {
    synchronized (AuctionService.class) {
      instance = loadedInstance;
    }
  }

  // Dùng để lấy toàn bộ dữ liệu Map auctions để lưu xuống file
  public Map<String, Auction> getAuctionsMap() {
    return this.auctions; // auctions là cái Map<String, Auction>
  }

  /**
   *  * Dùng để khôi phục dữ liệu sau khi đọc từ file .dat lên.
   *  
   */
  public void setAuctions(Map<String, Auction> loadedAuctions) {
    if (loadedAuctions != null) {
      this.auctions.clear(); // Xóa sạch dữ liệu trắng hiện tại
      this.auctions.putAll(loadedAuctions); // Đổ toàn bộ dữ liệu từ file vào

      // Khôi phục lại lịch trình cho các đối tượng vừa được nạp vào Map
      recoverScheduledTasks();
    }
  }

  /**
   *  * shutdown.
   *  
   */
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
      auction.closeAuction();
    }

    // QUAN TRỌNG: Lưu toàn bộ dữ liệu hiện tại xuống file .dat
    // Điều này đảm bảo giá thầu và trạng thái phiên đấu giá được bảo toàn
    try {
      if (this.dataStorage != null) {
        this.dataStorage.saveData();
      }
      System.out.println("[SERVICE] Dữ liệu đã được lưu an toàn vào file .dat.");
    } catch (Exception e) {
      System.err.println("[SERVICE ERROR] Không thể lưu dữ liệu khi shutdown: " + e.getMessage());
    }
  }

  /**
   * Xóa phiên đấu giá — chỉ Admin mới được gọi.
   */
  public boolean deleteAuction(String auctionId) {
    Auction a = auctions.get(auctionId);
    if (a == null) {
      return false;
    }
    a.closeAuction();
    auctions.remove(auctionId);
    if (this.dataStorage != null) {
      this.dataStorage.saveData();
    }
    System.out.println("[ADMIN] Đã xóa phiên: " + auctionId);
    return true;
  }

  // Trong AuctionService.java
  /**
   *  * Ngắt bỏ mọi obersever.
   *  
   */
  public void removeObserverFromAll(Observer obs) {
    for (Auction auction : auctions.values()) {
      auction.removeObserver(obs);
    }
  }
}
