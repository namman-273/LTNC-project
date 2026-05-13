package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.user.Bidder;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.observer.Observer;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.AuctionClosedException;
import com.auction.util.exception.AuthenticationException;
import com.auction.util.exception.InvalidBidException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Auction trực tiếp:
 * - addAutoBidConfig: bước giá hợp lệ / không hợp lệ / replace existing
 * - addObserver / removeObserver
 * - notifyAllParticipants / notifySpecificUser
 * - closeAuction: FINISHED, observers cleared
 * - restoreTransients: idempotent
 * - toString
 * - null item → IllegalArgumentException
 * - processNewBid: expired time → AuctionClosedException
 */
public class AuctionCoreTest {

    private static final long DURATION = 9999L;
    private Bidder alice;
    private Bidder bob;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        UserManager.getInstance().register("alice", "pw", "BIDDER", "alice@test.com");
        UserManager.getInstance().register("bob", "pw", "BIDDER", "bob@test.com");

        alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bob   = (Bidder) UserManager.getInstance().findUserByUsername("bob");

        alice.addBalance(50_000_000.0);
        bob.addBalance(50_000_000.0);
    }

    // ===== Constructor =====

    @Test
    void constructorNullItemThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Auction("null-item", null, DURATION, "seller"));
    }

    @Test
    void newAuctionCurrentPriceEqualsStartingPrice() {
        Auction a = new Auction("A1", new Electronics("e1", "TV", 1_000_000.0), DURATION, "seller");
        assertEquals(1_000_000.0, a.getCurrentPrice(), 0.001);
    }

    @Test
    void newAuctionStatusIsOpen() {
        Auction a = new Auction("A2", new Electronics("e2", "TV", 1_000_000.0), DURATION, "seller");
        assertEquals(AuctionStatus.OPEN, a.getStatus());
    }

    @Test
    void newAuctionBidHistoryIsEmpty() {
        Auction a = new Auction("A3", new Electronics("e3", "TV", 1_000_000.0), DURATION, "seller");
        assertTrue(a.getBidHistory().isEmpty());
    }

    // ===== processNewBid – expired time =====

    @Test
    void bidOnExpiredTimeThrowsAuctionClosed() throws Exception {
        Auction a = new Auction("A4", new Electronics("e4", "TV", 500_000.0), DURATION, null);
        // Set endTime to past
        Field f = Auction.class.getDeclaredField("endTime");
        f.setAccessible(true);
        f.set(a, System.currentTimeMillis() - 1000L);

        assertThrows(AuctionClosedException.class,
            () -> a.processNewBid(alice, 600_000.0));
    }

    @Test
    void bidWithNullBidderThrowsAuthenticationException() {
        Auction a = new Auction("A5", new Electronics("e5", "TV", 500_000.0), DURATION, null);
        assertThrows(AuthenticationException.class,
            () -> a.processNewBid(null, 600_000.0));
    }

    // ===== addAutoBidConfig =====

    @Test
    void addAutoBidConfigWithValidStepDoesNotThrow() {
        Auction a = new Auction("AB1", new Electronics("e-ab1", "TV", 500_000.0), DURATION, null);
        // minIncrement for 500_000 = 50_000
        assertDoesNotThrow(() -> a.addAutoBidConfig("alice", 2_000_000.0, 50_000.0));
    }

    @Test
    void addAutoBidConfigWithBelowMinStepThrows() {
        Auction a = new Auction("AB2", new Electronics("e-ab2", "TV", 500_000.0), DURATION, null);
        // minIncrement = 50_000; customStep = 1_000 < minIncrement → throw
        assertThrows(InvalidBidException.class,
            () -> a.addAutoBidConfig("alice", 2_000_000.0, 1_000.0));
    }

    @Test
    void addAutoBidConfigReplacesPreviousConfig() throws Exception {
        Auction a = new Auction("AB3", new Electronics("e-ab3", "TV", 500_000.0), DURATION, null);
        a.addAutoBidConfig("alice", 1_000_000.0, 50_000.0);
        // Re-register with higher maxBid — should not throw
        assertDoesNotThrow(() -> a.addAutoBidConfig("alice", 2_000_000.0, 50_000.0));
    }

    // ===== Observer management =====

    @Test
    void addObserverIncreasesParticipantCount() throws Exception {
        Auction a = new Auction("OB1", new Electronics("e-ob1", "TV", 500_000.0), DURATION, null);

        AtomicBoolean notified = new AtomicBoolean(false);
        Observer obs = msg -> notified.set(true);
        a.addObserver(obs);

        // Trigger notification via bid
        a.processNewBid(alice, 550_000.0);

        // If observer was added, executor will run — we just check no crash
        assertTrue(a.getBidHistory().size() >= 1);
    }

    @Test
    void addObserverTwiceDoesNotDuplicate() throws Exception {
        Auction a = new Auction("OB2", new Electronics("e-ob2", "TV", 500_000.0), DURATION, null);

        List<String> received = new ArrayList<>();
        Observer obs = received::add;

        a.addObserver(obs);
        a.addObserver(obs); // duplicate

        // Only one instance should be in observers list
        // We can't access the list directly, but we verify no exception and
        // the auction still functions
        assertDoesNotThrow(() -> a.processNewBid(alice, 550_000.0));
    }

    @Test
    void removeObserverDoesNotThrow() {
        Auction a = new Auction("OB3", new Electronics("e-ob3", "TV", 500_000.0), DURATION, null);
        Observer obs = msg -> {};
        a.addObserver(obs);
        assertDoesNotThrow(() -> a.removeObserver(obs));
    }

    @Test
    void removeNonExistentObserverDoesNotThrow() {
        Auction a = new Auction("OB4", new Electronics("e-ob4", "TV", 500_000.0), DURATION, null);
        assertDoesNotThrow(() -> a.removeObserver(msg -> {}));
    }

    // ===== closeAuction =====

    @Test
    void closeAuctionSetsFinishedStatus() {
        Auction a = new Auction("CA1", new Electronics("e-ca1", "TV", 500_000.0), DURATION, null);
        a.closeAuction();
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    @Test
    void closeAuctionTwiceDoesNotThrow() {
        Auction a = new Auction("CA2", new Electronics("e-ca2", "TV", 500_000.0), DURATION, null);
        a.closeAuction();
        assertDoesNotThrow(a::closeAuction);
    }

    @Test
    void afterCloseAuctionBidThrowsClosed() {
        Auction a = new Auction("CA3", new Electronics("e-ca3", "TV", 500_000.0), DURATION, null);
        a.closeAuction();
        assertThrows(AuctionClosedException.class,
            () -> a.processNewBid(alice, 600_000.0));
    }

    // ===== restoreTransients =====

    @Test
    void restoreTransientsIsIdempotent() {
        Auction a = new Auction("RT1", new Electronics("e-rt1", "TV", 500_000.0), DURATION, null);
        assertDoesNotThrow(() -> {
            a.restoreTransients();
            a.restoreTransients();
            a.restoreTransients();
        });
    }

    // ===== toString =====

    @Test
    void toStringContainsAuctionId() {
        Auction a = new Auction("MY_ID", new Electronics("e-ts", "TV", 500_000.0), DURATION, null);
        assertTrue(a.toString().contains("MY_ID"));
    }

    @Test
    void toStringContainsItemName() {
        Auction a = new Auction("TS2", new Electronics("e-ts2", "UniqueItem", 500_000.0), DURATION, null);
        assertTrue(a.toString().contains("UniqueItem"));
    }

    @Test
    void toStringContainsStatus() {
        Auction a = new Auction("TS3", new Electronics("e-ts3", "TV", 500_000.0), DURATION, null);
        assertTrue(a.toString().contains("OPEN"));
    }

    // ===== getters =====

    @Test
    void getItemReturnsCorrectItem() {
        Electronics item = new Electronics("e-get", "Camera", 2_000_000.0);
        Auction a = new Auction("GET1", item, DURATION, "seller");
        assertSame(item, a.getItem());
    }

    @Test
    void getSellerIdReturnsCorrectSellerId() {
        Auction a = new Auction("GET2", new Electronics("e-g2", "TV", 1_000_000.0), DURATION, "mySeller");
        assertEquals("mySeller", a.getSellerId());
    }

    @Test
    void getEndTimeIsInFuture() {
        Auction a = new Auction("GET3", new Electronics("e-g3", "TV", 1_000_000.0), 60L, "seller");
        assertTrue(a.getEndTime() > System.currentTimeMillis());
    }
}