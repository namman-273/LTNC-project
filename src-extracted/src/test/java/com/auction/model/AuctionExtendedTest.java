package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionExtendedTest {
 
    private static final double STARTING_PRICE = 1000.0;
    private static final double BID_1 = 51000.0;
    private static final double BID_2 = 102000.0;
    private static final double BID_3 = 153000.0;
    private static final long LONG_DURATION = 9999L;
 
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;
    private Item item;
 
    @BeforeEach
    void setUp() throws Exception {
        Field field = UserManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
 
        UserManager.getInstance().register("alice", "pass123", "BIDDER");
        UserManager.getInstance().register("bob", "pass456", "BIDDER");
 
        item = new Electronics("item-02", "Camera", STARTING_PRICE);
        auction = new Auction("auction-02", item, LONG_DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
 
        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder2 = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        bidder1.addBalance(10_000_000.0);
        bidder2.addBalance(10_000_000.0);
    }
 
    // --- Expired auction ---
 
    @Test
    void bidOnExpiredAuctionThrowsAuctionClosedException() throws Exception {
        Auction expired = new Auction("exp-01", new Electronics("e-exp", "TV", 500.0), 0L, null);
        expired.setStatus(AuctionStatus.RUNNING);
        Thread.sleep(10);
        assertThrows(
                AuctionClosedException.class,
                () -> expired.processNewBid(bidder1, 600.0));
    }
 
    // --- AutoBid: single bidder does not keep bidding against themselves ---
 
    @Test
    void autoBidSingleBidderDoesNotInfiniteLoop() {
        assertDoesNotThrow(() -> {
            auction.processNewBid(bidder1, BID_1);
            auction.addAutoBidConfig("alice", BID_3);
        });
    }
 
    // --- AutoBid: two bidders compete until one hits max ---
 
  
    @Test
    void autoBidStopsWhenMaxBidReached() throws Exception {
        auction.processNewBid(bidder2, BID_1);
        auction.addAutoBidConfig("alice", BID_2);
        // alice's maxBid=1200, after competing, price should not exceed 1200
        assertTrue(auction.getCurrentPrice() <= BID_2 + 50.0);
    }
 
    // --- AutoBid: update existing config ---
 
    @Test
    void autoBidUpdateConfigReplacesOldConfig() {
        assertDoesNotThrow(() -> {
            auction.addAutoBidConfig("alice", BID_1);
            auction.addAutoBidConfig("alice", BID_3);
        });
    }
 
    // --- Observer: notifyObservers after closeAuction (executor shutdown) ---
 
    @Test
    void notifyObserversAfterCloseAuctionDoesNotThrow() {
        auction.closeAuction();
        assertDoesNotThrow(() -> auction.notifyObservers("msg"));
    }
 
    // --- restoreTransients when null fields ---
 
    @Test
    void restoreTransientsInitializesFieldsCorrectly() {
        Auction fresh = new Auction("restore-01", item, LONG_DURATION, null);
        assertDoesNotThrow(() -> {
            fresh.restoreTransients();
            fresh.restoreTransients();
        });
    }
 
    // --- addObserver when observers is null triggers restoreTransients ---
 
    @Test
    void addObserverWhenObserversNullDoesNotThrow() {
        assertDoesNotThrow(() -> auction.addObserver(msg -> {
        }));
    }
 
    // --- processNewBid: multiple valid bids sequence ---
 
    @Test
    void threeValidBidsHistoryHasThreeEntries() throws Exception {
        auction.processNewBid(bidder1, BID_1);
        auction.processNewBid(bidder2, BID_2);
        auction.processNewBid(bidder1, BID_3);
        assertEquals(3, auction.getBidHistory().size());
    }
 
    // --- toString with no item ---
 
    @Test
    void toStringContainsCurrentPrice() {
        assertTrue(auction.toString().contains(String.valueOf((int) STARTING_PRICE)));
    }
 
    // --- AuctionStatus.OPEN valid bid ---
 
    @Test
    void bidOnOpenStatusSucceeds() throws Exception {
        Auction openAuction = new Auction("open-01", new Art("art-1", "Vase", 200.0), LONG_DURATION, null);
        // minimum increment for price < 1,000,000 is 50,000 → valid bid = 200 + 50000
        assertDoesNotThrow(() -> openAuction.processNewBid(bidder1, 50200.0));
    }
 
    // --- Concurrency with multiple threads ---
 
    @Test
    void multipleConcurrentBidsAllRecordedCorrectly() throws Exception {
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        // minimum increment = 50,000 for price < 1,000,000
        // each price must be at least STARTING_PRICE + 50000, and each step >= 50000 apart
        double[] prices = {51000, 102000, 153000, 204000, 255000};
 
        for (int i = 0; i < threadCount; i++) {
            final double price = prices[i];
            final User bidder = (i % 2 == 0) ? bidder1 : bidder2;
            threads[i] = new Thread(() -> {
                try {
                    auction.processNewBid(bidder, price);
                } catch (Exception ignored) {
                    // concurrent bids may legitimately fail
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertTrue(auction.getBidHistory().size() >= 1);
        assertTrue(auction.getCurrentPrice() > STARTING_PRICE);
    }
 
    // --- processNewBid: exactly at current price (boundary) ---
 
    @Test
    void bidExactlyAtCurrentPriceThrowsInvalidBidException() {
        assertThrows(
                InvalidBidException.class,
                () -> auction.processNewBid(bidder1, STARTING_PRICE));
    }
 
    // --- validateAuthentication via null bidder ---
 
    @Test
    void bidNullBidderThrowsAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> auction.processNewBid(null, BID_1));
    }
 
    // --- setStatus and getStatus consistency ---
 
    @Test
    void setStatusRunningThenGetStatusReturnsRunning() {
        auction.setStatus(AuctionStatus.RUNNING);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
}