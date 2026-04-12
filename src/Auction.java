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
    private double currentPrice;
    private AuctionStatus status;
    private final ReentrantLock lock = new ReentrantLock();
    private List<Observer> observers;

    public Auction(String id, Item item) {
        super(id);
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.history = new ArrayList<>();
        //AuctionStatus của thằng nger mặc định là phiên mới nên open
        this.status=AuctionStatus.OPEN;
    }

    public void addObserver(Observer obs) { observers.add(obs); }

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
    public void processNewBid(Bidder bidder, double bidAmount) {
        lock.lock();
        try {

            if (this.status != AuctionStatus.RUNNING) {
                System.out.println("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
                return;
            }

            if (bidAmount > currentPrice) {
                this.currentPrice = bidAmount;
                BidTransaction tx = new BidTransaction(bidder, bidAmount);
                history.add(tx);

                System.out.println("Đặt giá thành công: " + bidAmount + " bởi " + bidder.getName());
                //notify khi co bid moi dc dat thanh cong
                notifyObservers("Giá mới: " + bidAmount + " bởi " + bidder.getName());
            } else {
                System.out.println("Giá đặt " + bidAmount + " phải cao hơn giá hiện tại " + currentPrice);
            }
        } finally {
            // Phải luôn luôn mở khóa trong khối finally
            // đảm bảo  có lỗi xảy ra, khóa vẫn  giải phóng cho người sau
            lock.unlock();
        }
    }
}
