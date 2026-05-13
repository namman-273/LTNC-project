package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.entities.Auction;
import com.auction.model.entities.AutoBid;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.user.Bidder;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.auctionhelpers.AutoBidProcessor;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.InvalidBidException;

import java.lang.reflect.Field;
import java.util.PriorityQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test AutoBidProcessor.executeAutoBids và AutoBid.compareTo.
 */
public class AutoBidProcessorTest {

    private static final long DURATION = 9999L;

    private Bidder bidder1;
    private Bidder bidder2;
    private AutoBidProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        UserManager.getInstance().register("alice", "pw", "BIDDER", "alice@test.com");
        UserManager.getInstance().register("bob", "pw", "BIDDER", "bob@test.com");

        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder2 = (Bidder) UserManager.getInstance().findUserByUsername("bob");

        bidder1.addBalance(50_000_000.0);
        bidder2.addBalance(50_000_000.0);

        processor = new AutoBidProcessor();
    }

    // ===== AutoBid.compareTo =====

    @Test
    void higherMaxBidComesFirst() {
        AutoBid high = new AutoBid("alice", 5_000_000.0, 100_000.0);
        AutoBid low  = new AutoBid("bob",   3_000_000.0, 100_000.0);
        assertTrue(high.compareTo(low) < 0, "Higher maxBid should rank first (smaller compareTo result)");
    }

    @Test
    void lowerMaxBidComesLast() {
        AutoBid high = new AutoBid("alice", 5_000_000.0, 100_000.0);
        AutoBid low  = new AutoBid("bob",   3_000_000.0, 100_000.0);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    void equalMaxBidEarlierTimestampWins() throws InterruptedException {
        AutoBid first  = new AutoBid("alice", 5_000_000.0, 100_000.0);
        Thread.sleep(2); // ensure different timestamp
        AutoBid second = new AutoBid("bob",   5_000_000.0, 100_000.0);
        assertTrue(first.compareTo(second) < 0, "Earlier registration should win on tie");
    }

    @Test
    void priorityQueuePollsHighestMaxBidFirst() {
        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        pq.add(new AutoBid("bob",   2_000_000.0, 100_000.0));
        pq.add(new AutoBid("alice", 5_000_000.0, 100_000.0));

        assertEquals("alice", pq.poll().getBidderId());
    }

    // ===== executeAutoBids =====

    @Test
    void nullQueueDoesNothing() {
        Auction a = buildAuction(500_000.0);
        assertDoesNotThrow(() -> processor.executeAutoBids(null, a, (u, p) -> {}));
    }

    @Test
    void emptyQueueDoesNothing() {
        Auction a = buildAuction(500_000.0);
        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        assertDoesNotThrow(() -> processor.executeAutoBids(pq, a, (u, p) -> {}));
    }

    @Test
    void autoBidWithinBudgetPlacesBid() throws Exception {
        Auction a = buildAuction(500_000.0);

        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        // maxBid=1_000_000; bidStep=100_000; currentPrice=500_000 → nextPrice=600_000
        pq.add(new AutoBid("alice", 1_000_000.0, 100_000.0));

        processor.executeAutoBids(pq, a, (user, price) -> {
            user.deductBalance(price);
            a.getBidHistory().add(new com.auction.model.entities.BidTransaction(user, price));
            try {
                Field f = Auction.class.getDeclaredField("currentPrice");
                f.setAccessible(true);
                f.set(a, price);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });

        assertFalse(a.getBidHistory().isEmpty(), "AutoBid must place at least one bid");
    }

    @Test
    void autoBidExceedingBudgetIsSkipped() {
        Auction a = buildAuction(1_000_000.0);

        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        // maxBid=999_999 < currentPrice+step=1_100_000 → should not bid
        pq.add(new AutoBid("alice", 999_999.0, 100_000.0));

        processor.executeAutoBids(pq, a, (user, price) -> {
            fail("Should not place bid when budget exceeded");
        });

        assertTrue(a.getBidHistory().isEmpty());
    }

    @Test
    void autoBidWithInsufficientBalanceStopsGracefully() {
        Auction a = buildAuction(500_000.0);

        // Drain bidder1's balance to 0
        bidder1.deductBalance(bidder1.getBalance());

        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        pq.add(new AutoBid("alice", 2_000_000.0, 100_000.0));

        // Should not throw – processor catches InvalidBidException internally
        assertDoesNotThrow(() -> processor.executeAutoBids(pq, a,
            (user, price) -> {
                if (!user.deductBalance(price)) {
                    throw new InvalidBidException("Số dư không đủ");
                }
            }));
    }

    @Test
    void twoBotsSameAuctionHigherMaxBidWins() throws Exception {
        Auction a = buildAuction(500_000.0);

        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        pq.add(new AutoBid("alice", 3_000_000.0, 100_000.0)); // stronger
        pq.add(new AutoBid("bob",   1_000_000.0, 100_000.0)); // weaker

        // Run a limited version: track who places bids
        final String[] lastBidder = {null};

        processor.executeAutoBids(pq, a, (user, price) -> {
            user.deductBalance(price);
            lastBidder[0] = user.getUsername();
            a.getBidHistory().add(new com.auction.model.entities.BidTransaction(user, price));
            try {
                Field f = Auction.class.getDeclaredField("currentPrice");
                f.setAccessible(true);
                f.set(a, price);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });

        // alice has higher maxBid, so she should win the auto-bid round
        assertEquals("alice", lastBidder[0],
            "Bidder with higher maxBid should win auto-bid");
    }

    // ===== helper =====

    private Auction buildAuction(double startingPrice) {
        Auction a = new Auction("AUTO-001",
            new Electronics("e-auto", "Item", startingPrice),
            DURATION, "seller");
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}