package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AuctionCloseTest {

    private static final long DURATION = 9999L;

    @Test
    void closeAuctionSetsStatusFinished() {
        Auction a = openAuction();
        a.closeAuction();
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    @Test
    void closeAuctionTwiceIsSafe() {
        Auction a = openAuction();
        a.closeAuction();
        assertDoesNotThrow(a::closeAuction);
    }

    @Test
    void toStringContainsId() {
        assertTrue(openAuction().toString().contains("close-test"));
    }

    @Test
    void notifyObserversCallsObserver() throws InterruptedException {
        Auction a = openAuction();
        boolean[] called = {false};
        a.addObserver(msg -> called[0] = true);
        a.notifyObservers("PING");
        Thread.sleep(200);
        assertTrue(called[0]);
    }

    @Test
    void nullItemThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new Auction("bad", null, DURATION, "seller"));
    }

    private Auction openAuction() {
        Auction a = new Auction("close-test",
            new Electronics("e-ct", "Laptop", 500_000.0), DURATION, "seller-x");
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }
}