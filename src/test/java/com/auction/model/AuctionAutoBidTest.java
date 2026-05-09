package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;

public class AuctionAutoBidTest {

    private static final long DURATION = 9999L;

    @BeforeEach
    void setUp() throws Exception {
        Field f = UserManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob", "pw", "BIDDER");
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        Bidder bob   = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        alice.addBalance(50_000_000.0);
        bob.addBalance(50_000_000.0);
    }

    @Test
    void addAutoBidConfigStepBelowMinimumThrows() {
        Auction a = openAuction(500_000.0);
        assertThrows(InvalidBidException.class,
            () -> a.addAutoBidConfig("alice", 2_000_000.0, 1.0));
    }

    @Test
    void addAutoBidConfigValidStepDoesNotThrow() {
        Auction a = openAuction(500_000.0);
        assertDoesNotThrow(() -> a.addAutoBidConfig("alice", 2_000_000.0, 50_000.0));
    }

    @Test
    void autoBidRaisesCurrentPrice() throws Exception {
        Auction a = openAuction(500_000.0);
        a.addAutoBidConfig("alice", 1_000_000.0, 50_000.0);
        assertTrue(a.getCurrentPrice() > 500_000.0);
    }

    @Test
    void autoBidDoesNotExceedMaxBid() throws Exception {
        Auction a = openAuction(500_000.0);
        a.addAutoBidConfig("alice", 700_000.0, 50_000.0);
        assertTrue(a.getCurrentPrice() <= 700_000.0);
    }

    @Test
    void sellerAutoBidDoesNotRaisePrice() throws Exception {
        Auction a = new Auction("ab-self",
            new Electronics("e1", "Phone", 500_000.0), DURATION, "alice");
        a.setStatus(AuctionStatus.RUNNING);
        assertDoesNotThrow(() -> a.addAutoBidConfig("alice", 2_000_000.0, 50_000.0));
        assertEquals(500_000.0, a.getCurrentPrice(), 0.001);
    }

    @Test
    void updatingAutoBidConfigReplacesOld() throws Exception {
        Auction a = openAuction(500_000.0);
        a.addAutoBidConfig("alice", 600_000.0, 50_000.0);
        assertDoesNotThrow(() -> a.addAutoBidConfig("alice", 900_000.0, 50_000.0));
    }

    private Auction openAuction(double startPrice) {
        Auction a = new Auction("ab-" + (long) startPrice,
            new Electronics("e-ab", "Item", startPrice), DURATION, "seller");
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}