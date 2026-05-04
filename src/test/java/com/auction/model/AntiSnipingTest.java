package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.service.UserManager;
 
public class AntiSnipingTest {
 
    private static final double STARTING_PRICE = 1000.0;
    private static final long SAFE_DURATION_MINUTES = 10L;
    private static final long SHORT_DURATION_MINUTES = 5L;
    private static final long NEAR_END_MILLIS = 30_000L;
    private static final long EXTENSION_MILLIS = 120_000L;
    private static final int MAX_EXTENSIONS = 3;
    private static final double BID_INCREMENT = 51000.0;
    private static final double SMALL_INCREMENT = 51000.0;
 
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
 
    @Test
    void antiSnipingExtendsTimeWhenBidAtLastMinute() throws Exception {
        Item snipingItem = new Electronics("sniping-item-1", "Tablet", STARTING_PRICE);
        Auction snipingAuction = new Auction("sniping-1", snipingItem, SAFE_DURATION_MINUTES, null);
        snipingAuction.setStatus(AuctionStatus.RUNNING);
 
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        long nearEndTime = System.currentTimeMillis() + NEAR_END_MILLIS;
        endTimeField.set(snipingAuction, nearEndTime);
        long originalEndTime = (long) endTimeField.get(snipingAuction);
 
        snipingAuction.processNewBid(bidder1, STARTING_PRICE + BID_INCREMENT);
 
        assertEquals(originalEndTime + EXTENSION_MILLIS, snipingAuction.getEndTime(),
            "End time must be extended by 2 minutes when bid placed in last minute");
    }
 
    @Test
    void antiSnipingDoesNotExceedMaxExtensions() throws Exception {
        Item limitItem = new Electronics("limit-item-1", "Camera", STARTING_PRICE);
        Auction limitAuction = new Auction("limit-1", limitItem, SHORT_DURATION_MINUTES, null);
        limitAuction.setStatus(AuctionStatus.RUNNING);
 
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        Field countField = Auction.class.getDeclaredField("extensionCount");
        countField.setAccessible(true);
 
        countField.set(limitAuction, MAX_EXTENSIONS);
        long nearEndTime = System.currentTimeMillis() + NEAR_END_MILLIS;
        endTimeField.set(limitAuction, nearEndTime);
        long timeBeforeBid = (long) endTimeField.get(limitAuction);
 
        limitAuction.processNewBid(bidder1, STARTING_PRICE + SMALL_INCREMENT);
 
        assertEquals(timeBeforeBid, limitAuction.getEndTime(),
            "End time must not change when MAX_EXTENSIONS already reached");
    }
 
    @Test
    void antiSnipingDoesNotTriggerWhenTimeIsSufficient() throws Exception {
        Item safeItem = new Electronics("safe-item-1", "Monitor", STARTING_PRICE);
        Auction safeAuction = new Auction("safe-1", safeItem, SAFE_DURATION_MINUTES, null);
        safeAuction.setStatus(AuctionStatus.RUNNING);
        long initialEndTime = safeAuction.getEndTime();
 
        safeAuction.processNewBid(bidder1, STARTING_PRICE + SMALL_INCREMENT);
 
        assertEquals(initialEndTime, safeAuction.getEndTime(),
            "End time must not change when remaining time is above threshold");
    }
 
    @Test
    void antiSnipingExtensionCountIncrements() throws Exception {
        Item item = new Electronics("snip-item-2", "Phone", STARTING_PRICE);
        Auction auction = new Auction("snip-2", item, SAFE_DURATION_MINUTES, null);
        auction.setStatus(AuctionStatus.RUNNING);
 
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        Field countField = Auction.class.getDeclaredField("extensionCount");
        countField.setAccessible(true);
 
        endTimeField.set(auction, System.currentTimeMillis() + NEAR_END_MILLIS);
        auction.processNewBid(bidder1, STARTING_PRICE + BID_INCREMENT);
 
        int count = (int) countField.get(auction);
        assertEquals(1, count, "Extension count must be 1 after first anti-sniping trigger");
    }
 
    @Test
    void antiSnipingCanExtendMultipleTimesUpToMax() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob", "pw", "BIDDER");
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        Bidder bob   = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        alice.addBalance(10_000_000.0);
        bob.addBalance(10_000_000.0);
 
        Item item = new Electronics("snip-item-3", "Watch", STARTING_PRICE);
        Auction auction = new Auction("snip-3", item, SAFE_DURATION_MINUTES, null);
        auction.setStatus(AuctionStatus.RUNNING);
 
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        Field countField = Auction.class.getDeclaredField("extensionCount");
        countField.setAccessible(true);
 
        double price = STARTING_PRICE;
        for (int i = 0; i < MAX_EXTENSIONS; i++) {
            endTimeField.set(auction, System.currentTimeMillis() + NEAR_END_MILLIS);
            price += BID_INCREMENT;
            Bidder bidder = (i % 2 == 0) ? alice : bob;
            auction.processNewBid(bidder, price);
        }
 
        int count = (int) countField.get(auction);
        assertEquals(MAX_EXTENSIONS, count,
            "Extension count must reach MAX_EXTENSIONS after repeated last-minute bids");
    }
}