package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.exception.InvalidBidException;
import com.auction.factory.ItemFactory;
import com.auction.service.UserManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers missed branches in Auction + CreateItem:
 * - NaN / Infinite bid amount → InvalidBidException
 * - seller bids own auction → InvalidBidException
 * - autoBidQueue null → restoreTransients in addAutoBidConfig
 * - readObject (deserialization) → restoreTransients called
 * - CreateItem.getFactory fallback to VehicleFactory
 * - Bidder watchlist null init path
 */
public class AuctionMissedBranchTest {

    private static final double STARTING_PRICE = 1000.0;
    private static final long DURATION = 9999L;

    private Auction auction;
    private Bidder bidder;
    private Seller seller;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob_seller", "pw", "SELLER");
        bidder = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        seller = (Seller) UserManager.getInstance().findUserByUsername("bob_seller");
        bidder.addBalance(10_000_000.0);
        seller.addBalance(10_000_000.0);

        auction = new Auction("test-1", new Electronics("it-1", "TV", STARTING_PRICE),
            DURATION, "bob_seller");
        auction.setStatus(AuctionStatus.RUNNING);
    }

    // --- NaN / Infinite bid ---

    @Test
    void bidNaNThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder, Double.NaN),
            "NaN bid amount must throw InvalidBidException");
    }

    @Test
    void bidPositiveInfinityThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder, Double.POSITIVE_INFINITY),
            "Infinite bid amount must throw InvalidBidException");
    }

    @Test
    void bidNegativeInfinityThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder, Double.NEGATIVE_INFINITY),
            "Negative infinite bid must throw InvalidBidException");
    }

    // --- Seller bids own auction ---

    @Test
    void sellerCannotBidOwnAuction() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(seller, STARTING_PRICE + 50000.0),
            "Seller must not be able to bid on their own auction");
    }

    // --- readObject deserialization ---

    @Test
    void deserializedAuctionHasRestoredTransients() throws Exception {
        auction.setStatus(AuctionStatus.RUNNING);
        auction.processNewBid(bidder, STARTING_PRICE + 50000.0);

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(auction);
        oos.close();

        // Deserialize → triggers readObject → restoreTransients
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Auction restored = (Auction) ois.readObject();
        ois.close();

        assertNotNull(restored);
        assertEquals(auction.getCurrentPrice(), restored.getCurrentPrice(), 0.001);
        assertEquals(1, restored.getBidHistory().size());

        // After restore, can still add observers and notify without NPE
        assertDoesNotThrow(() -> {
            restored.addObserver(msg -> {});
            restored.notifyObservers("ping");
        });
    }

    // --- autoBidQueue null path in addAutoBidConfig ---

    @Test
    void addAutoBidConfigWhenQueueNullRestoresTransients() throws Exception {
        // Force autoBidQueue to null to trigger restoreTransients path
        Field queueField = Auction.class.getDeclaredField("autoBidQueue");
        queueField.setAccessible(true);
        queueField.set(auction, null);

        Bidder bot = new Bidder("bot1", "pw");
        bot.addBalance(10_000_000.0);
        UserManager.getInstance().getUsers().put("bot1", bot);

        assertDoesNotThrow(() -> auction.addAutoBidConfig("bot1", STARTING_PRICE + 500000.0));
    }

    // --- autoBid error branch: bot with insufficient balance ---

    @Test
    void autoBidStopsWhenBotHasInsufficientBalance() throws Exception {
        // Bot with very low balance — can't cover the next bid
        Bidder poorBot = new Bidder("poorbot", "pw");
        poorBot.addBalance(1.0); // only 1 VND
        UserManager.getInstance().getUsers().put("poorbot", poorBot);

        // Manual bid first so there's a current price
        auction.processNewBid(bidder, STARTING_PRICE + 50000.0);

        // Register auto-bid with maxBid > currentPrice but bot has no money
        assertDoesNotThrow(() -> auction.addAutoBidConfig("poorbot", STARTING_PRICE + 500000.0));
        // Bot's auto-bid attempt should fail gracefully (not crash)
    }

    // --- CreateItem.getFactory fallback ---

    @Test
    void createItemGetFactoryArtReturnsArtFactory() {
        ItemFactory f = CreateItem.getFactory("ART");
        assertNotNull(f);
        Item item = f.create("id1", "Painting", 500.0);
        assertTrue(item instanceof Art);
    }

    @Test
    void createItemGetFactoryElectronicsReturnsElectronicsFactory() {
        ItemFactory f = CreateItem.getFactory("ELECTRONICS");
        assertNotNull(f);
        Item item = f.create("id2", "Phone", 200.0);
        assertTrue(item instanceof Electronics);
    }

    @Test
    void createItemGetFactoryUnknownTypeReturnsVehicleFactory() {
        // Any unknown type → VehicleFactory (default branch)
        ItemFactory f = CreateItem.getFactory("UNKNOWN");
        assertNotNull(f);
        Item item = f.create("id3", "Car", 1000.0);
        assertTrue(item instanceof Vehicle);
    }

    @Test
    void createItemGetFactoryCaseInsensitive() {
        ItemFactory f = CreateItem.getFactory("art");
        assertNotNull(f);
        assertTrue(f.create("id4", "Sculpture", 300.0) instanceof Art);
    }

    // --- Bidder: watchlist null init path ---

    @Test
    void bidderWatchlistNullInitOnRemove() throws Exception {
        Bidder fresh = new Bidder("freshbidder", "pw");
        // Force watchlist to null
        Field wField = Bidder.class.getDeclaredField("watchlist");
        wField.setAccessible(true);
        wField.set(fresh, null);

        // removeFromWatchlist must not throw even with null watchlist
        assertDoesNotThrow(() -> fresh.removeFromWatchlist("any-id"));
    }

    @Test
    void bidderGetWatchlistNullInitReturnsEmptySet() throws Exception {
        Bidder fresh = new Bidder("freshbidder2", "pw");
        Field wField = Bidder.class.getDeclaredField("watchlist");
        wField.setAccessible(true);
        wField.set(fresh, null);

        // getWatchlist must re-init and return empty set
        assertNotNull(fresh.getWatchlist());
        assertTrue(fresh.getWatchlist().isEmpty());
    }

    // --- AuctionService.createNewAuction ---

    @Test
    void createNewAuctionAddsToService() throws Exception {
        Field asField = com.auction.service.AuctionService.class.getDeclaredField("instance");
        asField.setAccessible(true);
        asField.set(null, null);
        com.auction.service.AuctionService service = com.auction.service.AuctionService.getInstance();

        int before = service.getAllAuctions().size();
        service.createNewAuction("ELECTRONICS", "Laptop", 500000.0, 30L, "bob_seller");
        assertEquals(before + 1, service.getAllAuctions().size());
    }

    // --- Auction.getSellerId ---

    @Test
    void getSellerIdReturnsCorrectSeller() {
        assertEquals("bob_seller", auction.getSellerId());
    }

   
}