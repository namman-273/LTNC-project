package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
 
import com.auction.exception.AuctionClosedException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionStateMachineTest {
 
    private static final double STARTING_PRICE = 1000.0;
    private static final double BID_1 = 51000.0;
    private static final long DURATION = 9999L;
 
    private Bidder bidder1;
 
    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder1.addBalance(10_000_000.0);
    }
 
    // --- valid transitions ---
 
    @Test
    void newAuctionStatusIsOpen() {
        Auction a = new Auction("st-1", new Electronics("e1", "TV", STARTING_PRICE), DURATION, null);
        assertEquals(AuctionStatus.OPEN, a.getStatus());
    }
 
    @Test
    void transitionOpenToRunningAllowed() {
        Auction a = new Auction("st-2", new Electronics("e2", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);
        assertEquals(AuctionStatus.RUNNING, a.getStatus());
    }
 
    @Test
    void transitionRunningToFinished() {
        Auction a = new Auction("st-3", new Electronics("e3", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);
        a.setStatus(AuctionStatus.FINISHED);
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }
 
    @Test
    void transitionFinishedToPaid() {
        Auction a = new Auction("st-4", new Electronics("e4", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.FINISHED);
        a.setStatus(AuctionStatus.PAID);
        assertEquals(AuctionStatus.PAID, a.getStatus());
    }
 
    @Test
    void transitionAnyStatusToCanceled() {
        Auction a = new Auction("st-5", new Electronics("e5", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);
        a.setStatus(AuctionStatus.CANCELED);
        assertEquals(AuctionStatus.CANCELED, a.getStatus());
    }
 
    @Test
    void closeAuctionAlwaysResultsInFinished() {
        Auction a = new Auction("st-6", new Electronics("e6", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);
        a.closeAuction();
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }
 
    // --- bids: valid vs invalid by status ---
 
    @Test
    void bidInOpenStatusSucceeds() {
        Auction a = new Auction("st-7", new Electronics("e7", "TV", STARTING_PRICE), DURATION, null);
        assertEquals(AuctionStatus.OPEN, a.getStatus());
        assertDoesNotThrow(() -> a.processNewBid(bidder1, BID_1),
            "OPEN auction must accept valid bid");
    }
 
    @Test
    void bidInRunningStatusSucceeds() {
        Auction a = new Auction("st-8", new Electronics("e8", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.RUNNING);
        assertDoesNotThrow(() -> a.processNewBid(bidder1, BID_1),
            "RUNNING auction must accept valid bid");
    }
 
    @Test
    void bidOnFinishedAuctionThrows() {
        Auction a = new Auction("st-9", new Electronics("e9", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.FINISHED);
        assertThrows(AuctionClosedException.class,
            () -> a.processNewBid(bidder1, BID_1));
    }
 
    @Test
    void bidOnPaidAuctionThrows() {
        Auction a = new Auction("st-10", new Electronics("e10", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.PAID);
        assertThrows(AuctionClosedException.class,
            () -> a.processNewBid(bidder1, BID_1));
    }
 
    @Test
    void bidOnCanceledAuctionThrows() {
        Auction a = new Auction("st-11", new Electronics("e11", "TV", STARTING_PRICE), DURATION, null);
        a.setStatus(AuctionStatus.CANCELED);
        assertThrows(AuctionClosedException.class,
            () -> a.processNewBid(bidder1, BID_1));
    }
 
   
}