package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test getMinimumIncrement (private) thông qua processNewBid,
 * và rule seller không được tự bid.
 * Đây là logic nghiệp vụ quan trọng nhưng chưa có test tier-by-tier.
 */
public class AuctionBidIncrementTest {

    private static final long DURATION = 9999L;

    private Bidder bidder;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        bidder = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder.addBalance(100_000_000.0);
    }

    // --- Tier 1: price < 1,000,000 → bước 50,000 ---

    @Test
    void tier1BidExactly50000AboveStartingSucceeds() throws Exception {
        Auction a = auctionAt(500_000.0);
        a.processNewBid(bidder, 550_000.0);
        assertEquals(550_000.0, a.getCurrentPrice(), 0.001);
    }

    @Test
    void tier1BidOneBelow50000IncrementThrows() {
        Auction a = auctionAt(500_000.0);
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(bidder, 549_999.0));
    }

    @Test
    void tier1BoundaryAt999999PriceStep50000() throws Exception {
        Auction a = auctionAt(900_000.0);
        a.processNewBid(bidder, 950_000.0);
        assertEquals(950_000.0, a.getCurrentPrice(), 0.001);
    }

    // --- Tier 2: 1,000,000 ≤ price < 5,000,000 → bước 100,000 ---

    @Test
    void tier2BidExactly100000AboveCurrentSucceeds() throws Exception {
        Auction a = auctionAt(1_000_000.0);
        a.processNewBid(bidder, 1_100_000.0);
        assertEquals(1_100_000.0, a.getCurrentPrice(), 0.001);
    }

    @Test
    void tier2BidBelow100000IncrementThrows() {
        Auction a = auctionAt(1_000_000.0);
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(bidder, 1_099_999.0));
    }

    @Test
    void tier2BoundaryAt4999999PriceStep100000() throws Exception {
        Auction a = auctionAt(4_900_000.0);
        a.processNewBid(bidder, 5_000_000.0);
        assertEquals(5_000_000.0, a.getCurrentPrice(), 0.001);
    }

    // --- Tier 3: 5,000,000 ≤ price < 10,000,000 → bước 250,000 ---

    @Test
    void tier3BidExactly250000AboveCurrentSucceeds() throws Exception {
        Auction a = auctionAt(5_000_000.0);
        a.processNewBid(bidder, 5_250_000.0);
        assertEquals(5_250_000.0, a.getCurrentPrice(), 0.001);
    }

    @Test
    void tier3BidBelow250000IncrementThrows() {
        Auction a = auctionAt(5_000_000.0);
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(bidder, 5_249_999.0));
    }

    // --- Tier 4: price >= 10,000,000 → bước 500,000 ---

    @Test
    void tier4BidExactly500000AboveCurrentSucceeds() throws Exception {
        Auction a = auctionAt(10_000_000.0);
        a.processNewBid(bidder, 10_500_000.0);
        assertEquals(10_500_000.0, a.getCurrentPrice(), 0.001);
    }

    @Test
    void tier4BidBelow500000IncrementThrows() {
        Auction a = auctionAt(10_000_000.0);
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(bidder, 10_499_999.0));
    }

    @Test
    void tier4HighPriceStep500000Succeeds() throws Exception {
        Auction a = auctionAt(50_000_000.0);
        a.processNewBid(bidder, 50_500_000.0);
        assertEquals(50_500_000.0, a.getCurrentPrice(), 0.001);
    }

    // --- Seller không được tự bid ---

    @Test
    void sellerCannotBidOwnAuction() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("bob", "pw", "BIDDER");
        Bidder bob = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        bob.addBalance(100_000_000.0);

        // sellerId = "bob"
        Auction a = new Auction("seller-bid-1",
            new Electronics("e-s", "TV", 500_000.0), DURATION, "bob");
        a.setStatus(AuctionStatus.RUNNING);

        InvalidBidException ex = assertThrows(InvalidBidException.class,
            () -> a.processNewBid(bob, 550_000.0));
        assertTrue(ex.getMessage().contains("chính mình") || ex.getMessage().contains("own"),
            "Error message must mention self-bidding");
    }

    // --- Helper ---

    private Auction auctionAt(double startingPrice) {
        Auction a = new Auction("inc-" + (long) startingPrice,
            new Electronics("e-" + (long) startingPrice, "Item", startingPrice),
            DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}