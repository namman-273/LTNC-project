package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;


public class AutoBidTest {

    private static final double MAX_BID_HIGH = 200000.0;
    private static final double MAX_BID_LOW = 100000.0;
    private static final double STARTING_PRICE = 1000.0;
    private static final long DURATION = 9999L;
    private static final long TIMESTAMP_GAP_MS = 5L;

    // --- compareTo ---

    @Test
    void getBidderIdReturnsCorrectId() {
        AutoBid autoBid = new AutoBid("alice", MAX_BID_HIGH);
        assertEquals("alice", autoBid.getBidderId());
    }

    @Test
    void getMaxBidReturnsCorrectValue() {
        AutoBid autoBid = new AutoBid("bob", MAX_BID_HIGH);
        assertEquals(MAX_BID_HIGH, autoBid.getMaxBid());
    }

    @Test
    void compareToHigherMaxBidHasPriority() {
        AutoBid high = new AutoBid("alice", MAX_BID_HIGH);
        AutoBid low = new AutoBid("bob", MAX_BID_LOW);
        assertTrue(high.compareTo(low) < 0);
    }

    @Test
    void compareToLowerMaxBidHasLowerPriority() {
        AutoBid high = new AutoBid("alice", MAX_BID_HIGH);
        AutoBid low = new AutoBid("bob", MAX_BID_LOW);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    void compareToSameMaxBidEarlierTimestampHasPriority() throws InterruptedException {
        AutoBid first = new AutoBid("alice", MAX_BID_HIGH);
        Thread.sleep(TIMESTAMP_GAP_MS);
        AutoBid second = new AutoBid("bob", MAX_BID_HIGH);
        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    void compareToSameMaxBidLaterTimestampHasLowerPriority() throws InterruptedException {
        AutoBid first = new AutoBid("alice", MAX_BID_HIGH);
        Thread.sleep(TIMESTAMP_GAP_MS);
        AutoBid second = new AutoBid("bob", MAX_BID_HIGH);
        assertTrue(second.compareTo(first) > 0);
    }

    @Test
    void compareToSameObjectReturnsZero() {
        AutoBid autoBid = new AutoBid("alice", MAX_BID_HIGH);
        assertEquals(0, autoBid.compareTo(autoBid));
    }

    // --- executeAutoBids logic ---

    private Auction buildAuction(String auctionId, String itemId) throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob", "pw", "BIDDER");
        UserManager.getInstance().findUserByUsername("alice").addBalance(10_000_000.0);
        UserManager.getInstance().findUserByUsername("bob").addBalance(10_000_000.0);
        Item item = new Electronics(itemId, "Device", STARTING_PRICE);
        Auction auction = new Auction(auctionId, item, DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
        return auction;
    }

    @Test
    void autoBidDoesNotExceedMaxBid() throws Exception {
        Auction auction = buildAuction("ab-2", "e-ab2");
        Bidder bob = (Bidder) UserManager.getInstance().findUserByUsername("bob");

        auction.addAutoBidConfig("alice", 1050.0);
        auction.processNewBid(bob, 51000.0);

        assertEquals(51000.0, auction.getCurrentPrice(),
            "Auto-bid must not exceed maxBid");
    }

    @Test
    void autoBidUpdateConfigReplacesOldConfig() throws Exception {
        Auction auction = buildAuction("ab-6", "e-ab6");
        Bidder bob = (Bidder) UserManager.getInstance().findUserByUsername("bob");

        auction.addAutoBidConfig("alice", 51000.0);
        auction.addAutoBidConfig("alice", 200000.0);
        auction.processNewBid(bob, 101000.0);

        assertTrue(auction.getCurrentPrice() > 101000.0,
            "Updated auto-bid config with higher maxBid must take effect");
    }

    // --- BID REFUND: người bid trước phải được hoàn tiền ---

    @Test
    void previousBidderGetsRefundWhenOutbid() throws Exception {
        Auction auction = buildAuction("refund-1", "e-ref1");
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        Bidder bob = (Bidder) UserManager.getInstance().findUserByUsername("bob");

        double aliceStart = alice.getBalance();
        auction.processNewBid(alice, 51000.0);
        // Lúc này alice đã bị trừ 51000
        assertEquals(aliceStart - 51000.0, alice.getBalance(), 0.001);

        // Bob vượt giá → alice được hoàn tiền
        auction.processNewBid(bob, 102000.0);
        assertEquals(aliceStart, alice.getBalance(), 0.001,
            "Alice must get full refund after being outbid");
    }

    @Test
    void winnerBalanceDeductedByBidAmount() throws Exception {
        Auction auction = buildAuction("refund-2", "e-ref2");
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");

        double before = alice.getBalance();
        auction.processNewBid(alice, 51000.0);

        assertEquals(before - 51000.0, alice.getBalance(), 0.001,
            "Winner's balance must be deducted by bid amount");
    }
}