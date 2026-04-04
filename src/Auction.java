import java.util.ArrayList;
import java.util.List;
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

    public Auction(String id, Item item) {
        super(id);
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.history = new ArrayList<>();
    }
    public void processNewBid(Bidder bidder, double bidAmount) {
        if (bidAmount > currentPrice) {
            this.currentPrice = bidAmount;
            BidTransaction tx = new BidTransaction(bidder, bidAmount);
            history.add(tx);
            System.out.println("Đặt giá thành công: " + bidAmount + " bởi " + bidder.getUsername());
        } else {
            System.out.println("Giá đặt phải cao hơn giá hiện tại!");
        }
    }
}
