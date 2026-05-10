package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AutoBidTest {

    @Test
    void gettersReturnCorrectValues() {
        AutoBid ab = new AutoBid("user1", 5_000_000.0, 100_000.0);
        assertEquals("user1", ab.getBidderId());
        assertEquals(5_000_000.0, ab.getMaxBid(), 0.001);
        assertEquals(100_000.0, ab.getbidStep(), 0.001);
    }

    @Test
    void compareToHigherMaxBidComesFirst() {
        AutoBid high = new AutoBid("rich", 10_000_000.0, 500_000.0);
        AutoBid low  = new AutoBid("poor",  1_000_000.0, 100_000.0);
        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    void compareToSameMaxBidEarlierTimestampWins() throws InterruptedException {
        AutoBid first  = new AutoBid("a", 1_000_000.0, 50_000.0);
        Thread.sleep(5);
        AutoBid second = new AutoBid("b", 1_000_000.0, 50_000.0);
        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    void compareToSelf() {
        AutoBid ab = new AutoBid("x", 2_000_000.0, 100_000.0);
        assertEquals(0, ab.compareTo(ab));
    }
}