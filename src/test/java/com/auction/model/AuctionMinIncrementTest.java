package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers all 4 tiers of getMinimumIncrement via validateBidAmount:
 *   < 1_000_000  → step 50_000
 *   < 5_000_000  → step 100_000
 *   < 10_000_000 → step 250_000
 *   >= 10_000_000 → step 500_000
 */
public class AuctionMinIncrementTest {

    private static final long DUR = 9999L;
    private Bidder alice;

    @BeforeEach
    void setUp() throws Exception {
        Field f = UserManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        alice.addBalance(100_000_000.0);
    }

    // ── Tier 1: price < 1_000_000, step = 50_000 ─────────────

    @Test
    void tier1BidBelowMinIncrementThrows() {
        Auction a = open(500_000.0);
        // min = 500_000 + 50_000 = 550_000; bid 549_999 → invalid
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(alice, 549_999.0));
    }

    @Test
    void tier1BidAtExactMinIncrementSucceeds() {
        Auction a = open(500_000.0);
        assertDoesNotThrow(() -> a.processNewBid(alice, 550_000.0));
    }

    // ── Tier 2: price in [1_000_000, 5_000_000), step = 100_000 ──

    @Test
    void tier2BidBelowMinIncrementThrows() {
        Auction a = open(2_000_000.0);
        // min = 2_000_000 + 100_000 = 2_100_000; bid 2_099_999 → invalid
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(alice, 2_099_999.0));
    }

    @Test
    void tier2BidAtExactMinIncrementSucceeds() {
        Auction a = open(2_000_000.0);
        assertDoesNotThrow(() -> a.processNewBid(alice, 2_100_000.0));
    }

    // ── Tier 3: price in [5_000_000, 10_000_000), step = 250_000 ─

    @Test
    void tier3BidBelowMinIncrementThrows() {
        Auction a = open(6_000_000.0);
        // min = 6_000_000 + 250_000 = 6_250_000; bid 6_249_999 → invalid
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(alice, 6_249_999.0));
    }

    @Test
    void tier3BidAtExactMinIncrementSucceeds() {
        Auction a = open(6_000_000.0);
        assertDoesNotThrow(() -> a.processNewBid(alice, 6_250_000.0));
    }

    // ── Tier 4: price >= 10_000_000, step = 500_000 ──────────

    @Test
    void tier4BidBelowMinIncrementThrows() {
        Auction a = open(10_000_000.0);
        // min = 10_000_000 + 500_000 = 10_500_000; bid 10_499_999 → invalid
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(alice, 10_499_999.0));
    }

    @Test
    void tier4BidAtExactMinIncrementSucceeds() {
        Auction a = open(10_000_000.0);
        assertDoesNotThrow(() -> a.processNewBid(alice, 10_500_000.0));
    }

    // ── NaN / Infinite guards ─────────────────────────────────

    @Test
    void bidNaNThrows() {
        Auction a = open(500_000.0);
        assertThrows(Exception.class, () -> a.processNewBid(alice, Double.NaN));
    }

    @Test
    void bidInfiniteThrows() {
        Auction a = open(500_000.0);
        assertThrows(Exception.class, () -> a.processNewBid(alice, Double.POSITIVE_INFINITY));
    }

    // ── seller bidding own auction ────────────────────────────

    @Test
    void sellerBiddingOwnAuctionThrows() {
        UserManager.getInstance().register("sellerX", "pw", "SELLER");
        // Create a Bidder with same username as seller to bypass type check
        Bidder sellerAsBidder = new Bidder("sellerX", "h");
        sellerAsBidder.addBalance(10_000_000.0);
        Auction a = new Auction("self-bid",
            new Electronics("e", "Item", 500_000.0), DUR, "sellerX");
        a.setStatus(AuctionStatus.RUNNING);
        assertThrows(InvalidBidException.class,
            () -> a.processNewBid(sellerAsBidder, 550_000.0));
    }

    private Auction open(double price) {
        Auction a = new Auction("inc-" + (long) price,
            new Electronics("e-" + (long) price, "Item", price), DUR, "seller");
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}