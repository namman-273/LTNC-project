package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Covers BidTransaction: all getters, setters, toString.
 */
public class BidTransactionExtendedTest {

    private Bidder makeBidder(String name) {
        return new Bidder(name, "h");
    }

    @Test
    void gettersBidderAndAmount() {
        Bidder b = makeBidder("alice");
        BidTransaction tx = new BidTransaction(b, 550_000.0);
        assertSame(b, tx.getBidder());
        assertEquals(550_000.0, tx.getAmount(), 0.001);
    }

    @Test
    void timestampNotNull() {
        BidTransaction tx = new BidTransaction(makeBidder("u"), 100.0);
        assertNotNull(tx.getTimestamp());
    }

    @Test
    void timestampIsRecentlyCreated() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        BidTransaction tx = new BidTransaction(makeBidder("u"), 100.0);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(tx.getTimestamp().isAfter(before));
        assertTrue(tx.getTimestamp().isBefore(after));
    }

    @Test
    void setBidderUpdates() {
        BidTransaction tx = new BidTransaction(makeBidder("alice"), 500.0);
        Bidder bob = makeBidder("bob");
        tx.setBidder(bob);
        assertSame(bob, tx.getBidder());
    }

    @Test
    void setAmountUpdates() {
        BidTransaction tx = new BidTransaction(makeBidder("alice"), 500.0);
        tx.setAmount(999_999.0);
        assertEquals(999_999.0, tx.getAmount(), 0.001);
    }

    @Test
    void setTimestampUpdates() {
        BidTransaction tx = new BidTransaction(makeBidder("u"), 100.0);
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        tx.setTimestamp(newTime);
        assertEquals(newTime, tx.getTimestamp());
    }

    @Test
    void toStringContainsBidderUsername() {
        BidTransaction tx = new BidTransaction(makeBidder("charlie"), 750_000.0);
        assertTrue(tx.toString().contains("charlie"));
    }

    @Test
    void toStringContainsAmount() {
        BidTransaction tx = new BidTransaction(makeBidder("u"), 750_000.0);
        assertTrue(tx.toString().contains("750000"));
    }
}