package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuctionFinancialTest {

    private static final long DURATION = 9999L;
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
        alice.addBalance(20_000_000.0);
        bob.addBalance(20_000_000.0);
    }

    @Test
    void bidDeductsBalance() throws Exception {
        Auction a = open(500_000.0);
        double before = alice.getBalance();
        a.processNewBid(alice, 550_000.0);
        assertEquals(before - 550_000.0, alice.getBalance(), 0.001);
    }

    @Test
    void secondBidRefundsPrevious() throws Exception {
        Auction a = open(500_000.0);
        a.processNewBid(alice, 550_000.0);
        double aliceAfter = alice.getBalance();
        a.processNewBid(bob, 650_000.0);
        assertEquals(aliceAfter + 550_000.0, alice.getBalance(), 0.001);
    }

    @Test
    void bidOnFinishedThrows() {
        Auction a = open(500_000.0);
        a.setStatus(AuctionStatus.FINISHED);
        assertThrows(AuctionClosedException.class, () -> a.processNewBid(alice, 550_000.0));
    }

    @Test
    void nullUserThrowsAuth() {
        assertThrows(AuthenticationException.class,
            () -> open(500_000.0).processNewBid(null, 550_000.0));
    }

    private Auction open(double p) {
        Auction a = new Auction("fin-" + (long) p,
            new Electronics("e", "Item", p), DURATION, "seller");
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}