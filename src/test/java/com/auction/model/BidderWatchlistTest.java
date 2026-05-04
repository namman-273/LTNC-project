package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.service.AuctionService;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Bidder watchlist logic và AuctionService.getWatchlistForUser
 * - Cả hai đều bị 0% coverage trước đây.
 */
public class BidderWatchlistTest {

    private static final long DURATION = 9999L;
    private static final double PRICE = 500.0;

    private Bidder bidder;
    private AuctionService service;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        Field asField = AuctionService.class.getDeclaredField("instance");
        asField.setAccessible(true);
        asField.set(null, null);

        UserManager.getInstance().register("alice", "pw", "BIDDER");
        bidder = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        service = AuctionService.getInstance();
    }

    // --- Bidder.addToWatchlist ---

    @Test
    void addToWatchlistValidIdIsStored() {
        bidder.addToWatchlist("auction-1");
        assertTrue(bidder.getWatchlist().contains("auction-1"));
    }

    @Test
    void addToWatchlistNullIdIsIgnored() {
        bidder.addToWatchlist(null);
        assertTrue(bidder.getWatchlist().isEmpty());
    }

    @Test
    void addToWatchlistEmptyStringIsIgnored() {
        bidder.addToWatchlist("");
        assertTrue(bidder.getWatchlist().isEmpty());
    }

    @Test
    void addToWatchlistDuplicateIdStoredOnce() {
        bidder.addToWatchlist("auction-1");
        bidder.addToWatchlist("auction-1");
        assertEquals(1, bidder.getWatchlist().size());
    }

    @Test
    void addToWatchlistMultipleIdsAllStored() {
        bidder.addToWatchlist("auction-1");
        bidder.addToWatchlist("auction-2");
        assertEquals(2, bidder.getWatchlist().size());
    }

    // --- Bidder.removeFromWatchlist ---

    @Test
    void removeFromWatchlistExistingIdRemovesIt() {
        bidder.addToWatchlist("auction-1");
        bidder.removeFromWatchlist("auction-1");
        assertFalse(bidder.getWatchlist().contains("auction-1"));
    }

    @Test
    void removeFromWatchlistNonExistentIdDoesNotThrow() {
        bidder.addToWatchlist("auction-1");
        bidder.removeFromWatchlist("non-existent");
        assertEquals(1, bidder.getWatchlist().size());
    }

    @Test
    void removeFromWatchlistOnEmptyWatchlistDoesNotThrow() {
        bidder.removeFromWatchlist("anything");
        assertTrue(bidder.getWatchlist().isEmpty());
    }

    // --- Bidder.getWatchlist: trả về bản sao, không leak ref ---

    @Test
    void getWatchlistReturnsCopyNotDirectRef() {
        bidder.addToWatchlist("auction-1");
        bidder.getWatchlist().add("injected");
        assertFalse(bidder.getWatchlist().contains("injected"),
            "getWatchlist must return a copy, not expose internal set");
    }

    // --- AuctionService.getWatchlistForUser ---

    @Test
    void getWatchlistForUserBidderWithMatchingAuctionReturnsList() {
        Auction a = new Auction("wl-1", new Electronics("e-wl", "TV", PRICE), DURATION, null);
        service.addAuction(a);
        bidder.addToWatchlist("wl-1");

        List<Auction> result = service.getWatchlistForUser("alice");
        assertEquals(1, result.size());
        assertEquals("wl-1", result.get(0).getId());
    }

    @Test
    void getWatchlistForUserAuctionNotInServiceIsFiltered() {
        // ID trong watchlist nhưng auction không tồn tại trong service
        bidder.addToWatchlist("ghost-auction");
        List<Auction> result = service.getWatchlistForUser("alice");
        assertTrue(result.isEmpty());
    }

    @Test
    void getWatchlistForUserNonBidderReturnsEmpty() throws Exception {
        // Seller không có watchlist
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("bob", "pw", "SELLER");

        List<Auction> result = service.getWatchlistForUser("bob");
        assertTrue(result.isEmpty());
    }

    @Test
    void getWatchlistForUserNonExistentUsernameReturnsEmpty() {
        List<Auction> result = service.getWatchlistForUser("nobody");
        assertTrue(result.isEmpty());
    }

    @Test
    void getWatchlistForUserEmptyWatchlistReturnsEmpty() {
        List<Auction> result = service.getWatchlistForUser("alice");
        assertTrue(result.isEmpty());
    }
}