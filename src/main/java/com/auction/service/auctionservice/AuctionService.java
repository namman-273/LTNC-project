package com.auction.service.auctionservice;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Item;
import com.auction.model.observer.Observer;
import com.auction.util.core.DataManager;
import com.auction.util.core.IDataStorage;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * AuctionService sử dụng Facade Pattern.
 * Delegate các công việc cho các helper classes chuyên biệt.
 * Tuân thủ SOLID principles.
 */
public class AuctionService implements Serializable {
  private static final long serialVersionUID = 1L;

  private static volatile AuctionService instance;

  // Helper classes - mỗi class có một trách nhiệm riêng (SRP)
  private final AuctionRepository auctionRepository;
  private transient AuctionScheduler scheduler;
  private final AuctionFactory auctionFactory;
  private final PaymentProcessor paymentProcessor;
  private final AuctionNotificationService notificationService;
  private final WatchlistService watchlistService;
  private transient AuctionEndHandler endHandler;

  // ÁP DỤNG DIP: Khai báo Interface
  private transient IDataStorage dataStorage;

  private AuctionService(IDataStorage dataStorage) {
    this.dataStorage = dataStorage;
    
    // Khởi tạo các helper classes
    this.auctionRepository = new AuctionRepository();
    this.scheduler = new AuctionScheduler();
    this.auctionFactory = new AuctionFactory();
    this.paymentProcessor = new PaymentProcessor();
    this.notificationService = new AuctionNotificationService();
    this.watchlistService = new WatchlistService(auctionRepository);
    this.endHandler = new AuctionEndHandler(auctionRepository, scheduler, 
        paymentProcessor, notificationService, dataStorage);
  }

  /**
   * Áp dụng singleton với double-check locking.
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
   * Lấy watchlist của user.
   */
  public List<Auction> getWatchlistForUser(String username) {
    return watchlistService.getWatchlistForUser(username);
  }

  /**
   * Tạo phiên đấu giá mới.
   */
  public synchronized void createNewAuction(String itemType, String itemName, 
      double startingPrice, long durationMinutes, String sellerId, 
      String description, String imageUrl) {
    
    // Tạo auction mới sử dụng Factory
    Auction newAuction = auctionFactory.createAuction(itemType, itemName, 
        startingPrice, durationMinutes, sellerId, description, imageUrl);

    // Lưu vào repository
    auctionRepository.add(newAuction.getId(), newAuction);

    // Lưu xuống file
    saveData();

    // Lên lịch tự động đóng
    scheduler.scheduleAuctionEnd(newAuction.getId(), durationMinutes, 
        () -> endAuction(newAuction.getId()));
  }

  /**
   * Khôi phục lại toàn bộ scheduler khi hệ thống khởi động lại.
   * FIX: Truyền đúng endAuction handler thay vì lambda rỗng.
   */
  private void recoverScheduledTasks() {
    scheduler.recoverScheduledTasks(auctionRepository, 
        auctionId -> endAuction(auctionId));
  }

  /**
   * FIX LỖI: Singleton bị phá khi deserialize.
   * Java (JVM) sẽ tự động quét qua class AuctionService xem có 
   * hàm nào tên là readResolve() hay không. Nếu có, JVM sẽ ngầm kích hoạt hàm này.
   * (khi he thong doc file .dat)
   */
  protected Object readResolve() {
    // Khi load từ file, gán instance hiện tại
    instance = this;
    
    // Khôi phục transient fields
    if (this.dataStorage == null) {
      this.dataStorage = DataManager.getInstance();
    }
    
    if (this.scheduler == null) {
      this.scheduler = new AuctionScheduler();
    }

    // Tạo lại endHandler với dependencies
    this.endHandler = new AuctionEndHandler(auctionRepository, scheduler, 
        paymentProcessor, notificationService, dataStorage);

    // Khôi phục scheduled tasks
    recoverScheduledTasks();

    return instance;
  }

  /**
   * Kết thúc auction tự động (hết thời gian).
   */
  public void endAuction(String auctionId) {
    endHandler.endAuction(auctionId);
  }

  /**
   * Kết thúc auction bởi Admin (đóng sớm, hoàn tiền).
   */
  public void endAuctionByAdmin(String auctionId) {
    endHandler.endAuctionByAdmin(auctionId);
  }

  /**
   * Lấy auction theo ID.
   */
  public Auction getAuctionById(String id) {
    return auctionRepository.findById(id);
  }

  /**
   * Lấy tất cả auctions.
   */
  public Collection<Auction> getAllAuctions() {
    return auctionRepository.getAllAuctions();
  }

  /**
   * Lấy Item trong auction.
   */
  public Item getItemInAuction(String auctionId) {
    Auction auction = auctionRepository.findById(auctionId);
    return (auction != null) ? auction.getItem() : null;
  }

  /**
   * Set instance (dùng khi load từ file).
   */
  public static void setInstance(AuctionService loadedInstance) {
    synchronized (AuctionService.class) {
      instance = loadedInstance;
    }
  }

  /**
   * Lấy toàn bộ Map auctions để lưu xuống file.
   */
  public Map<String, Auction> getAuctionsMap() {
    return auctionRepository.getAll();
  }

  /**
   * Khôi phục dữ liệu sau khi đọc từ file.
   */
  public void setAuctions(Map<String, Auction> loadedAuctions) {
    if (loadedAuctions != null) {
      auctionRepository.replaceAll(loadedAuctions);
      
      // Đảm bảo scheduler được khởi tạo
      if (scheduler == null) {
        scheduler = new AuctionScheduler();
      }
      
      // Khôi phục lại lịch trình
      recoverScheduledTasks();
    }
  }

  /**
   * Shutdown service gracefully.
   */
  public void shutdown() {
    System.out.println("[SERVICE] Đang tiến hành dọn dẹp và lưu dữ liệu...");

    // Dừng scheduler
    if (scheduler != null) {
      scheduler.shutdown();
    }

    // Đóng tất cả auctions
    for (Auction auction : auctionRepository.getAllAuctions()) {
      auction.closeAuction();
    }

    // Lưu dữ liệu
    try {
      saveData();
      System.out.println("[SERVICE] Dữ liệu đã được lưu an toàn vào file .dat.");
    } catch (Exception e) {
      System.err.println("[SERVICE ERROR] Không thể lưu dữ liệu khi shutdown: " 
          + e.getMessage());
    }
  }

  /**
   * Xóa phiên đấu giá - chỉ Admin.
   */
  public boolean deleteAuction(String auctionId) {
    Auction auction = auctionRepository.findById(auctionId);
    if (auction == null) {
      return false;
    }

    auction.closeAuction();
    auctionRepository.remove(auctionId);
    saveData();
    
    System.out.println("[ADMIN] Đã xóa phiên: " + auctionId);
    return true;
  }

  /**
   * Ngắt bỏ mọi observer.
   */
  public void removeObserverFromAll(Observer obs) {
    for (Auction auction : auctionRepository.getAllAuctions()) {
      auction.removeObserver(obs);
    }
  }

  /**
   * Helper method để lưu dữ liệu.
   */
  private void saveData() {
    if (dataStorage != null) {
      dataStorage.saveData();
    }
  }
}