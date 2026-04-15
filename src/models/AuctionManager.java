package models;

import java.util.ArrayList;
import java.util.List;
public class AuctionManager {
    private static AuctionManager instance;
    private List<Auction> auctions;
    private AuctionManager() {
        auctions = new ArrayList<>();
    }
    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    public void addAuction(Auction auction) {
        auctions.add(auction);
    }
    public List<Auction> getAuctions() {
        return auctions;
    }
}
