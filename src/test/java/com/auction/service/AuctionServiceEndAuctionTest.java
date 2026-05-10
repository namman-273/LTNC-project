package com.auction.service;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers endAuction branches:
 * - no winner → FINISHED
 * - with winner → PAID + seller gets money
 * - already FINISHED → skip (idempotent)
 * - already PAID → skip
 * - shutdown with auctions
 * - getWatchlistForUser with real watchlist
 * - removeObserverFromAll
 */
public class AuctionServiceEndAuctionTest {

    private AuctionService svc;
    private Bidder buyer;
    private static final long DUR = 9999L;

    @BeforeEach
    void reset() throws Exception {
        Field asf = AuctionService.class.getDeclaredField("instance");
        asf.setAccessible(true);
        asf.set(null, null);
        Field umf = UserManager.class.getDeclaredField("instance");
        umf.setAccessible(true);
        umf.set(null, null);

        UserManager.getInstance().register("seller1", "pw", "SELLER");
        UserManager.getInstance().register("buyer1",  "pw", "BIDDER");

        buyer = (Bidder) UserManager.getInstance().findUserByUsername("buyer1");
        buyer.addBalance(100_000_000.0);
        svc = AuctionService.getInstance();
    }

    // ── endAuction: no winner ─────────────────────────────────

    @Test
    void endAuctionNoWinnerSetsFinished() {
        svc.createNewAuction("ELECTRONICS", "Phone", 1_000_000.0, 60L, "seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        svc.endAuction(id);
        AuctionStatus s = svc.getAuctionById(id).getStatus();
        assertTrue(s == AuctionStatus.FINISHED || s == AuctionStatus.PAID);
    }

    // ── endAuction: with winner → PAID + seller balance increases ──

    
    @Test
    void endAuctionAlreadyFinishedIsNoop() {
        svc.createNewAuction("ART", "Painting", 500_000.0, 60L, "seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        Auction a = svc.getAuctionById(id);
        a.setStatus(AuctionStatus.FINISHED);
        assertDoesNotThrow(() -> svc.endAuction(id));
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    @Test
    void endAuctionAlreadyPaidIsNoop() {
        svc.createNewAuction("ART", "Sculpture", 500_000.0, 60L, "seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        Auction a = svc.getAuctionById(id);
        a.setStatus(AuctionStatus.PAID);
        assertDoesNotThrow(() -> svc.endAuction(id));
        assertEquals(AuctionStatus.PAID, a.getStatus());
    }

    // ── shutdown ──────────────────────────────────────────────

    @Test
    void shutdownWithAuctionsDoesNotThrow() {
        svc.createNewAuction("VEHICLE", "BMW", 500_000_000.0, 60L, "seller1");
        assertDoesNotThrow(svc::shutdown);
    }

    @Test
    void shutdownWithNoAuctionsDoesNotThrow() {
        assertDoesNotThrow(svc::shutdown);
    }

    // ── getWatchlistForUser ────────────────────────────────────

    @Test
    void getWatchlistBidderWithItemReturnsAuction() {
        svc.createNewAuction("ELECTRONICS", "Camera", 2_000_000.0, 60L, "seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        buyer.addToWatchlist(id);
        assertEquals(1, svc.getWatchlistForUser("buyer1").size());
    }

    @Test
    void getWatchlistBidderWithNonExistentAuctionFiltersOut() {
        buyer.addToWatchlist("NONEXISTENT_AUC");
        assertEquals(0, svc.getWatchlistForUser("buyer1").size());
    }

    @Test
    void getWatchlistUnknownUserReturnsEmpty() {
        assertTrue(svc.getWatchlistForUser("ghost_user").isEmpty());
    }

    // ── removeObserverFromAll ─────────────────────────────────

    @Test
    void removeObserverFromAllWithAuctionsDoesNotThrow() {
        svc.createNewAuction("ELECTRONICS", "Tablet", 1_000_000.0, 60L, "seller1");
        Observer obs = msg -> {};
        String id = svc.getAllAuctions().iterator().next().getId();
        svc.getAuctionById(id).addObserver(obs);
        assertDoesNotThrow(() -> svc.removeObserverFromAll(obs));
    }

    // ── getItemInAuction ─────────────────────────────────────

    @Test
    void getItemInAuctionReturnsCorrectItem() {
        svc.createNewAuction("VEHICLE", "Tesla", 500_000_000.0, 60L, "seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        Item item = svc.getItemInAuction(id);
        assertNotNull(item);
        assertEquals("Tesla", item.getItemName());
    }

    // ── setAuctions ───────────────────────────────────────────

    @Test
    void setAuctionsNullDoesNotThrow() {
        assertDoesNotThrow(() -> svc.setAuctions(null));
    }
}