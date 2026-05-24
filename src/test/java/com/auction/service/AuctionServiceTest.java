package com.auction.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.observer.Observer;

import com.auction.service.auctionservice.AuctionService;
import com.auction.service.usermanger.UserManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for AuctionService public methods.
 */
public class AuctionServiceTest {

    private AuctionService auctionService;

    @BeforeEach
    void setUp() throws Exception {
        resetSingletons();
        auctionService = AuctionService.getInstance();
        UserManager.getInstance().register("defaultSeller", "pw", "SELLER", "defaultSeller@test.com");
    }

    @AfterEach
    void tearDown() {
        try {
            auctionService.shutdown();
        } catch (Exception ignored) {}
        cleanFiles();
    }

    // ===== Singleton =====

    @Test
    void getInstanceReturnsSameObject() {
        assertSame(AuctionService.getInstance(), AuctionService.getInstance());
    }

    @Test
    void getInstanceIsNotNull() {
        assertNotNull(auctionService);
    }

    // ===== createNewAuction =====

    @Test
    void createNewAuctionAddsToMap() {
        auctionService.createNewAuction("ELECTRONICS", "TV", 500_000.0, 9999L, "defaultSeller", "", "");
        assertFalse(auctionService.getAllAuctions().isEmpty());
    }

    @Test
    void createNewAuctionCreatesCorrectItem() {
        auctionService.createNewAuction("ELECTRONICS", "Laptop", 1_000_000.0, 9999L, "defaultSeller", "", "");
        Auction auction = auctionService.getAllAuctions().iterator().next();
        assertEquals("Laptop", auction.getItem().getItemName());
    }

    @Test
    void createNewAuctionStatusIsOpen() {
        auctionService.createNewAuction("ELECTRONICS", "Monitor", 500_000.0, 9999L, "defaultSeller", "", "");
        Auction auction = auctionService.getAllAuctions().iterator().next();
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void createNewAuctionStartingPriceCorrect() {
        auctionService.createNewAuction("ART", "Painting", 2_000_000.0, 9999L, "defaultSeller", "", "");
        Auction auction = auctionService.getAllAuctions().iterator().next();
        assertEquals(2_000_000.0, auction.getCurrentPrice(), 0.001);
    }

  
    @Test
    void getAuctionByIdReturnsCorrectAuction() {
        auctionService.createNewAuction("ELECTRONICS", "Camera", 1_500_000.0, 9999L, "defaultSeller", "", "");
        Auction auction = auctionService.getAllAuctions().iterator().next();
        String id = auction.getId();
        assertSame(auction, auctionService.getAuctionById(id));
    }

    @Test
    void getAuctionByIdNonExistentReturnsNull() {
        assertNull(auctionService.getAuctionById("NON_EXISTENT_ID"));
    }

    // ===== getAllAuctions =====

    @Test
    void getAllAuctionsEmptyInitially() {
        assertTrue(auctionService.getAllAuctions().isEmpty());
    }

    @Test
    void getAllAuctionsReturnsCollection() {
        assertNotNull(auctionService.getAllAuctions());
    }

    // ===== getItemInAuction =====

    @Test
    void getItemInAuctionReturnsItem() {
        auctionService.createNewAuction("VEHICLE", "Car", 50_000_000.0, 9999L, "defaultSeller", "", "");
        Auction a = auctionService.getAllAuctions().iterator().next();
        assertNotNull(auctionService.getItemInAuction(a.getId()));
    }

    @Test
    void getItemInAuctionNonExistentReturnsNull() {
        assertNull(auctionService.getItemInAuction("GHOST_ID"));
    }

    // ===== getAuctionsMap =====

    @Test
    void getAuctionsMapIsNotNull() {
        assertNotNull(auctionService.getAuctionsMap());
    }

    @Test
    void getAuctionsMapReflectsCreatedAuctions() {
        auctionService.createNewAuction("ELECTRONICS", "Phone", 5_000_000.0, 9999L, "defaultSeller", "", "");
        assertFalse(auctionService.getAuctionsMap().isEmpty());
    }

    // ===== setAuctions =====

    @Test
    void setAuctionsNullDoesNotThrow() {
        assertDoesNotThrow(() -> auctionService.setAuctions(null));
    }

    @Test
    void setAuctionsReplacesExistingMap() {
        Map<String, Auction> newMap = new HashMap<>();
        Auction a = new Auction("injected-1",
            new Electronics("e-inj", "Watch", 500_000.0), 9999L, null);
        newMap.put("injected-1", a);
        auctionService.setAuctions(newMap);
        assertNotNull(auctionService.getAuctionById("injected-1"));
    }

    @Test
    void setAuctionsClearsOldData() {
        auctionService.createNewAuction("ELECTRONICS", "OldItem", 500_000.0, 9999L, "defaultSeller", "", "");
        Map<String, Auction> empty = new HashMap<>();
        auctionService.setAuctions(empty);
        assertTrue(auctionService.getAllAuctions().isEmpty());
    }

    // ===== setInstance =====

   

   

    @Test
    void endAuctionNonExistentIdDoesNotThrow() {
        assertDoesNotThrow(() -> auctionService.endAuction("GHOST_ID"));
    }

    @Test
    void endAuctionWithFutureEndTimeReschedulesDoesNotThrow() {
        auctionService.createNewAuction("ELECTRONICS", "TV", 500_000.0, 9999L, "defaultSeller", "", "");
        Auction a = auctionService.getAllAuctions().iterator().next();
        // endTime is in the future → endAuction should reschedule and not change status
        assertDoesNotThrow(() -> auctionService.endAuction(a.getId()));
    }

    @Test
    void endAuctionWithPastEndTimeAndNoBidsSetsFinished() throws Exception {
        auctionService.createNewAuction("ELECTRONICS", "Radio", 500_000.0, 9999L, "defaultSeller", "", "");
        Auction a = auctionService.getAllAuctions().iterator().next();

        // Set endTime to the past
        setEndTimeToPast(a);

        auctionService.endAuction(a.getId());

        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

   
    @Test
    void endAuctionAlreadyFinishedDoesNotReprocess() throws Exception {
        auctionService.createNewAuction("ELECTRONICS", "TV2", 500_000.0, 9999L, "defaultSeller", "", "");
        Auction a = auctionService.getAllAuctions().iterator().next();
        setEndTimeToPast(a);
        auctionService.endAuction(a.getId()); // first call → FINISHED
        // second call should return early without re-processing
        assertDoesNotThrow(() -> auctionService.endAuction(a.getId()));
    }

    // ===== getWatchlistForUser =====

    @Test
    void getWatchlistForNonBidderReturnsEmpty() {
        List<Auction> result = auctionService.getWatchlistForUser("defaultSeller");
        assertTrue(result.isEmpty());
    }

    @Test
    void getWatchlistForUnknownUserReturnsEmpty() {
        List<Auction> result = auctionService.getWatchlistForUser("nobody");
        assertTrue(result.isEmpty());
    }

    @Test
    void getWatchlistForBidderWithEmptyWatchlistReturnsEmpty() {
        UserManager.getInstance().register("watcher", "pw", "BIDDER", "watcher@test.com");
        List<Auction> result = auctionService.getWatchlistForUser("watcher");
        assertTrue(result.isEmpty());
    }

    @Test
    void getWatchlistForBidderWithWatchedAuctionReturnsList() {
        UserManager.getInstance().register("watcher2", "pw", "BIDDER", "watcher2@test.com");
        Bidder watcher = (Bidder) UserManager.getInstance().findUserByUsername("watcher2");

        auctionService.createNewAuction("ELECTRONICS", "Watched", 500_000.0, 9999L, "defaultSeller", "", "");
        Auction a = auctionService.getAllAuctions().iterator().next();
        watcher.addToWatchlist(a.getId());

        List<Auction> watchlist = auctionService.getWatchlistForUser("watcher2");
        assertEquals(1, watchlist.size());
        assertSame(a, watchlist.get(0));
    }

    // ===== removeObserverFromAll =====

    @Test
    void removeObserverFromAllDoesNotThrow() {
        Observer fakeObs = msg -> {};
        assertDoesNotThrow(() -> auctionService.removeObserverFromAll(fakeObs));
    }

    @Test
    void removeObserverFromAllWithAuctionsDoesNotThrow() {
        auctionService.createNewAuction("ELECTRONICS", "ObsItem", 500_000.0, 9999L, "defaultSeller", "", "");
        Observer fakeObs = msg -> {};
        assertDoesNotThrow(() -> auctionService.removeObserverFromAll(fakeObs));
    }

    // ===== shutdown =====

    @Test
    void shutdownDoesNotThrow() {
        assertDoesNotThrow(() -> auctionService.shutdown());
    }

    @Test
    void shutdownCanBeCalledTwice() {
        auctionService.shutdown();
        assertDoesNotThrow(() -> auctionService.shutdown());
    }

    // ===== helpers =====

    private void setEndTimeToPast(Auction auction) throws Exception {
        Field f = Auction.class.getDeclaredField("endTime");
        f.setAccessible(true);
        f.set(auction, System.currentTimeMillis() - 1000L);
    }

    private void resetSingletons() throws Exception {
        // DataManager đã chuyển sang Holder idiom (không có field 'instance').
        // Không cần reset vì DataManager không giữ business state.
        Field as = AuctionService.class.getDeclaredField("instance");
        as.setAccessible(true);
        as.set(null, null);

        // Holder idiom: clear users via setUsers() thay vì reflection.
        UserManager.getInstance().setUsers(new java.util.HashMap<>());
    }

    private void cleanFiles() {
        new File("auctions.dat").delete();
        new File("users.dat").delete();
        new File("auctions.dat.tmp").delete();
        new File("users.dat.tmp").delete();
    }
}