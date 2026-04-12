public class AuctionSession {
    private String itemName;
    private double currentPrice;
    private boolean isOpen;

    public AuctionSession(String itemName, double startPrice) {
        this.itemName = itemName;
        this.currentPrice = startPrice;
        this.isOpen = true;
    }

    public String getItemName() {
        return itemName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        isOpen = false;
    }

    public void updatePrice(double newPrice) {
        this.currentPrice = newPrice;
    }
}