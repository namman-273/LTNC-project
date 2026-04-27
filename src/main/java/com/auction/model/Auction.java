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
    private static final long ONE_MINUTE_MS = 60000L;// ép kiểu sang Long
    private static final long TWO_MINUTES_MS = 120000L;
    private static final int MAX_EXTENSIONS = 3;

    private Item item;
    private List<BidTransaction> history;
    private AuctionStatus status;
    private double currentPrice;
    private long endTime; // Thời điểm kết thúc (ms)
    private PriorityQueue<AutoBid> autoBidQueue; // Hàng đợi ưu tiên
    private int extensionCount = 0;

    // transient: Những trường này sẽ không được lưu xuống file .dat
    private transient ReentrantLock lock;
    private transient List<Observer> observers;
    private transient ExecutorService notifyExecutor;

    public Auction(String id, Item item, long durationMinutes) {
        super(id);
        this.item = item;
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
        if (this.notifyExecutor == null || this.notifyExecutor.isShutdown())
            this.notifyExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
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
        if (observers == null)
            restoreTransients();
        observers.add(obs);
    }

    public void removeObserver(Observer obs) {
        if (observers != null)
            observers.remove(obs);
    }

    public void notifyObservers(String msg) {
        if (observers == null || observers.isEmpty())
            return;

        for (Observer observer : observers) {
            // Gửi thông báo bất đồng bộ
            notifyExecutor.submit(() -> {
                try {
                    observer.update(msg);
                } catch (Exception e) {
                    System.err.println("[OBSERVER ERROR] Không thể gửi thông báo cho 1 client: " + e.getMessage());
                    this.removeObserver(observer);
                }
            });
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
            handleAntiSniping();

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

            // Xóa cấu hình cũ của người này nếu có (để cập nhật cấu hình mới)
            autoBidQueue.removeIf(config -> config.getBidderId().equals(bidderId));

            this.autoBidQueue.add(new AutoBid(bidderId, maxBid, increment));
            System.out.println("SERVER: Đã nhận cấu hình Auto-bid cho " + bidderId);

            executeAutoBids();
        } finally {
            lock.unlock();
        }
    }

    // Định nghĩa logic máy tự động trả giá [MỤC NÂNG CAO]
    private void executeAutoBids() {
        if (autoBidQueue == null || autoBidQueue.isEmpty())
            return;

        // 1. Lấy người có quyền ưu tiên cao nhất ra
        AutoBid top = autoBidQueue.poll();

        String lastBidderId = history.isEmpty() ? "" : history.get(history.size() - 1).getBidder().getUsername();

        // 2. Nếu người đứng đầu hàng đợi chính là người đang giữ giá cao nhất
        if (top.getBidderId().equals(lastBidderId)) {
            // Kiểm tra xem có người thứ 2 trong hàng đợi để cạnh tranh không
            AutoBid nextCompetitor = autoBidQueue.peek();

            if (nextCompetitor != null) {
                // Nếu có đối thủ, ta tạm cất 'top' đi để xử lý đối thủ trước
                // giúp đẩy giá lên, rồi sau đó mới trả 'top' lại
                autoBidQueue.add(top);
                executeAutoBids(); // Đệ quy để xét đối thủ
                return;
            } else {
                // Nếu chỉ có một mình mình trong Queue thì dừng, đợi người khác bid tay
                autoBidQueue.add(top);
                return;
            }
        }

        // 3. Logic đặt giá cho người đang đuổi theo (giữ nguyên)
        double nextPrice = currentPrice + top.getIncrement();
        if (nextPrice <= top.getMaxBid()) {
            User user = UserManager.getInstance().findUserByUsername(top.getBidderId());
            if (user != null) {
                updateAuctionState(user, nextPrice);
                autoBidQueue.add(top); // Trả lại để chờ đối thủ vượt mặt
                executeAutoBids(); // Tiếp tục vòng đấu cho đến khi có người chạm giới hạn
            }
        }

        else {
            // TRƯỜNG HỢP DỪNG:
            // Nếu ngân sách không đủ (nextAutoPrice > maxBid), cấu hình sẽ KHÔNG được
            // add lại vào queue -> Tự động remove AutoBid
            System.out.println("AutoBid stop for: " + top.getBidderId());
        }
    }

    private void handleAntiSniping() {
        long timeLeft = this.endTime - System.currentTimeMillis();
        if (timeLeft > 0 && timeLeft < ONE_MINUTE_MS && extensionCount < MAX_EXTENSIONS) { // < 1 phút
            this.endTime += TWO_MINUTES_MS; // Cộng thêm 2 phút
            this.extensionCount++;
            notifyObservers("SNIPING|" + getId() + "|" + this.endTime + "|" + extensionCount);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject(); // Load các trường không phải transient
        restoreTransients(); // Tự động hồi sinh các trường bị null
    }
    public String getId() {
        return super.getId();
    }

<<<<<<< HEAD
    public double getCurrentPrice() {
        return currentPrice;
    }

    @Override
    public String toString() {
        return "id=" + getId()
            + ",itemName=" + (item != null ? item.getItemName() : "---")
            + ",currentPrice=" + currentPrice
            + ",status=" + status;}
=======
    // giải phóng tài nguyên khi phiên đấu giá kết thúc hoặc Server dừng
>>>>>>> 8cb0e0420272348ef8f8db187427647fb497fa05
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
            notifyExecutor.shutdown(); // Giải phóng 10 threads ngay lập tức
        }
    }

}
