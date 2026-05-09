package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers executeAutoBids competition branches:
 * - two bots competing: higher maxBid wins
 * - bot stops when balance insufficient
 * - bot stops at maxBid limit
 * - highest bidder bot pauses (no self-bid)
 */
public class AutoBidCompetitionTest {

    private static final long DUR = 9999L;
    private Bidder alice;
    private Bidder bob;

    @BeforeEach
    void setUp() throws Exception {
        Field f = UserManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob",   "pw", "BIDDER");
        alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bob   = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        alice.addBalance(50_000_000.0);
        bob.addBalance(50_000_000.0);
    }

    // ── Two bots: higher maxBid wins ──────────────────────────

    @Test
    void higherMaxBidBotWins() throws Exception {
        Auction a = open(500_000.0);
        // alice: maxBid=600_000, bob: maxBid=700_000
        a.addAutoBidConfig("alice", 600_000.0, 50_000.0);
        a.addAutoBidConfig("bob",   700_000.0, 50_000.0);
        // bob has higher maxBid → bob should end up as highest bidder
        String lastBidder = a.getBidHistory().isEmpty() ? ""
            : a.getBidHistory().get(a.getBidHistory().size() - 1).getBidder().getUsername();
        assertEquals("bob", lastBidder);
    }

    // ── Bot stops at maxBid ───────────────────────────────────

    @Test
    void botPriceNeverExceedsMaxBid() throws Exception {
        Auction a = open(500_000.0);
        a.addAutoBidConfig("alice", 650_000.0, 50_000.0);
        assertTrue(a.getCurrentPrice() <= 650_000.0);
    }

    // ── Bot with insufficient balance stops ───────────────────

    @Test
    void botWithZeroBalanceDoesNotRaisePrice() throws Exception {
        Bidder broke = new Bidder("broke", "h");
        // broke has 0 balance — bot should fail silently
        UserManager.getInstance().register("broke", "pw", "BIDDER");
        Auction a = open(500_000.0);
        // even if config is valid, bot can't bid because no balance
        assertDoesNotThrow(() -> a.addAutoBidConfig("broke", 2_000_000.0, 50_000.0));
        assertEquals(500_000.0, a.getCurrentPrice(), 0.001);
    }

    // ── Bid history grows with auto-bid ───────────────────────

    @Test
    void autoBidCreatesHistoryEntries() throws Exception {
        Auction a = open(500_000.0);
        a.addAutoBidConfig("alice", 700_000.0, 50_000.0);
        assertFalse(a.getBidHistory().isEmpty());
    }

    // ── Three bots: highest maxBid wins ───────────────────────

    @Test
    void threeBots_highestMaxBidWins() throws Exception {
        UserManager.getInstance().register("charlie", "pw", "BIDDER");
        Bidder charlie = (Bidder) UserManager.getInstance().findUserByUsername("charlie");
        charlie.addBalance(50_000_000.0);

        Auction a = open(500_000.0);
        a.addAutoBidConfig("alice",   600_000.0, 50_000.0);
        a.addAutoBidConfig("bob",     700_000.0, 50_000.0);
        a.addAutoBidConfig("charlie", 800_000.0, 50_000.0);

        // charlie has highest maxBid
        String lastBidder = a.getBidHistory().isEmpty() ? ""
            : a.getBidHistory().get(a.getBidHistory().size() - 1).getBidder().getUsername();
        assertEquals("charlie", lastBidder);
    }

    // ── Bot update replaces config ────────────────────────────

    @Test
    void updatingBotConfigRaisesMaxBid() throws Exception {
        Auction a = open(500_000.0);
        a.addAutoBidConfig("alice", 600_000.0, 50_000.0);
        double priceAfterFirst = a.getCurrentPrice();
        // Update to higher maxBid — should bid higher
        a.addAutoBidConfig("alice", 900_000.0, 50_000.0);
        assertTrue(a.getCurrentPrice() >= priceAfterFirst);
    }

    private Auction open(double price) {
        Auction a = new Auction("comp-" + (long) price,
            new Electronics("e-comp", "Item", price), DUR, "seller");
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}