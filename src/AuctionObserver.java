public interface AuctionObserver {
    void updatePrice(double newPrice, String lastBidderName);
}