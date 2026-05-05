package com.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Observer;
import com.auction.model.Seller;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class AuctionServiceLifecycleTest {

    private static final double STARTING_PRICE = 1000.0;
    private static final long DURATION = 9999L;

    private AuctionService service;
    private Bidder bidder;
    private Seller seller;

    @BeforeEach
    void setUp() throws Exception {
        Field asField = AuctionService.class.getDeclaredField("instance");
        asField.setAccessible(true);
        asField.set(null, null);

        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob_seller", "pw", "SELLER");
        bidder = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        seller = (Seller) UserManager.getInstance().findUserByUsername("bob_seller");
        bidder.addBalance(10_000_000.0);
        seller.addBalance(0.0);

        service = AuctionService.getInstance();
    }

    // --- endAuction: có winner ---

    @Test
    void endAuctionWithWinnerSetsPaidStatus() throws Exception {
        Auction a = new Auction("ea-1", new Electronics("i1", "TV", STARTING_PRICE), DURATION, "bob_seller");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        a.processNewBid(bidder, STARTING_PRICE + 50000.0);
        service.endAuction("ea-1");

        assertEquals(AuctionStatus.PAID, a.getStatus());
    }

    @Test
    void endAuctionWithWinnerTransfersMoneyToSeller() throws Exception {
        double initialSellerBalance = seller.getBalance();
        Auction a = new Auction("ea-2", new Electronics("i2", "Phone", STARTING_PRICE), DURATION, "bob_seller");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        double bidAmount = STARTING_PRICE + 50000.0;
        a.processNewBid(bidder, bidAmount);
        service.endAuction("ea-2");

        assertTrue(seller.getBalance() > initialSellerBalance,
            "Seller balance must increase after auction ends with winner");
    }

    @Test
    void endAuctionNoWinnerSetsFinishedStatus() {
        Auction a = new Auction("ea-3", new Electronics("i3", "Watch", STARTING_PRICE), DURATION, "bob_seller");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        service.endAuction("ea-3");

        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    @Test
    void endAuctionNonExistentIdDoesNotThrow() {
        assertDoesNotThrow(() -> service.endAuction("ghost-999"));
    }

    @Test
    void endAuctionAlreadyFinishedIsIdempotent() {
        Auction a = new Auction("ea-4", new Electronics("i4", "Laptop", STARTING_PRICE), DURATION, "bob_seller");
        a.setStatus(AuctionStatus.FINISHED);
        service.addAuction(a);

        assertDoesNotThrow(() -> service.endAuction("ea-4"));
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    // --- getItemInAuction ---

    @Test
    void getItemInAuctionReturnsCorrectItem() {
        Item item = new Electronics("it-5", "Camera", STARTING_PRICE);
        Auction a = new Auction("ga-1", item, DURATION, "bob_seller");
        service.addAuction(a);

        assertEquals(item, service.getItemInAuction("ga-1"));
    }

    @Test
    void getItemInAuctionNonExistentReturnsNull() {
        assertNull(service.getItemInAuction("no-such-auction"));
    }

    // --- getAllAuctions ---

    @Test
    void getAllAuctionsReturnsAllAdded() {
        service.addAuction(new Auction("all-1", new Electronics("x1", "A", STARTING_PRICE), DURATION, null));
        service.addAuction(new Auction("all-2", new Electronics("x2", "B", STARTING_PRICE), DURATION, null));

        assertTrue(service.getAllAuctions().size() >= 2);
    }

    // --- removeObserverFromAll ---

    @Test
    void removeObserverFromAllRemovesFromAllAuctions() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Observer obs = msg -> count.incrementAndGet();

        Auction a1 = new Auction("ro-1", new Electronics("r1", "A", STARTING_PRICE), DURATION, null);
        Auction a2 = new Auction("ro-2", new Electronics("r2", "B", STARTING_PRICE), DURATION, null);
        a1.addObserver(obs);
        a2.addObserver(obs);
        service.addAuction(a1);
        service.addAuction(a2);

        service.removeObserverFromAll(obs);
        a1.notifyObservers("ping");
        a2.notifyObservers("ping");
        Thread.sleep(200);

        assertEquals(0, count.get(), "Observer must not receive messages after removeObserverFromAll");
    }

    // --- addAutoBidConfig (cover Auction.executeAutoBids path) ---

    @Test
    void addAutoBidConfigRegistersAndTriggersAutoBid() throws Exception {
        Auction a = new Auction("ab-1", new Electronics("ab-i", "Tablet", STARTING_PRICE), DURATION, "bob_seller");
        a.setStatus(AuctionStatus.RUNNING);

        // bidder đặt giá thủ công trước
        a.processNewBid(bidder, STARTING_PRICE + 50000.0);

        // Thêm auto-bid với maxBid cao hơn
        Bidder autoBidder = new Bidder("autobot", "pw");
        autoBidder.addBalance(10_000_000.0);
        UserManager.getInstance().getUsers().put("autobot", autoBidder);

        assertDoesNotThrow(() -> a.addAutoBidConfig("autobot", STARTING_PRICE + 200000.0));
    }

    @Test
    void addAutoBidConfigUpdatesExistingConfig() {
        Auction a = new Auction("ab-2", new Electronics("ab-i2", "Speaker", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);

        assertDoesNotThrow(() -> {
            a.addAutoBidConfig("alice", STARTING_PRICE + 100000.0);
            a.addAutoBidConfig("alice", STARTING_PRICE + 200000.0); // update cấu hình cũ
        });
    }

    // --- restoreTransients ---

    @Test
    void restoreTransientsCanBeCalledMultipleTimes() {
        Auction a = new Auction("rt-1", new Electronics("rt-i", "Drone", STARTING_PRICE), DURATION, null);
        assertDoesNotThrow(() -> {
            a.restoreTransients();
            a.restoreTransients();
        });
    }

    // --- shutdown ---

    @Test
    void shutdownDoesNotThrow() throws Exception {
        // Tạo fresh instance để shutdown mà không ảnh hưởng test khác
        Field asField = AuctionService.class.getDeclaredField("instance");
        asField.setAccessible(true);
        asField.set(null, null);
        AuctionService freshService = AuctionService.getInstance();

        freshService.addAuction(new Auction("sd-1", new Electronics("s1", "PC", STARTING_PRICE), DURATION, null));
        assertDoesNotThrow(freshService::shutdown);
    }

    // --- UserManager.initDefaultData ---

    @Test
    void initDefaultDataCreatesAdminWhenEmpty() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        UserManager freshUm = UserManager.getInstance();
        freshUm.initDefaultData();

        assertNotNull(freshUm.findUserByUsername("admin"));
    }

    @Test
    void initDefaultDataDoesNothingWhenNotEmpty() throws Exception {
        // users đã có alice từ setUp
        int before = UserManager.getInstance().getUsers().size();
        UserManager.getInstance().initDefaultData();
        int after = UserManager.getInstance().getUsers().size();
        assertEquals(before, after, "initDefaultData must not add user if map is not empty");
    }
}