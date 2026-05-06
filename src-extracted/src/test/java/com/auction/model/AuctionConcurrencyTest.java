package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
 
public class AuctionConcurrencyTest {
 
    private static final double STARTING_PRICE = 1000.0;
    private static final double BID_1 = 51000.0;
    private static final double BID_2 = 102000.0;
    private static final double BID_3 = 153000.0;
    private static final double BID_4 = 204000.0;
    private static final double BID_5 = 255000.0;
    private static final long DURATION = 9999L;
 
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;
 
    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
 
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob", "pw", "BIDDER");
 
        Item item = new Electronics("item-c", "Laptop", STARTING_PRICE);
        auction = new Auction("auction-c", item, DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
 
        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder2 = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        bidder1.addBalance(10_000_000.0);
        bidder2.addBalance(10_000_000.0);
    }
 
    @Test
    void concurrentBidsAtLeastOneSucceeds() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
 
        Thread t1 = new Thread(() -> {
            try { latch.await(); auction.processNewBid(bidder1, BID_1); }
            catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { latch.await(); auction.processNewBid(bidder2, BID_2); }
            catch (Exception ignored) {}
        });
 
        t1.start(); t2.start();
        latch.countDown();
        t1.join(); t2.join();
 
        assertFalse(auction.getBidHistory().isEmpty(),
            "At least one bid must be recorded");
    }
 
    @Test
    void concurrentBidsPriceNeverDropsBelowStarting() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
 
        Thread t1 = new Thread(() -> {
            try { latch.await(); auction.processNewBid(bidder1, BID_1); }
            catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { latch.await(); auction.processNewBid(bidder2, BID_2); }
            catch (Exception ignored) {}
        });
 
        t1.start(); t2.start();
        latch.countDown();
        t1.join(); t2.join();
 
        assertTrue(auction.getCurrentPrice() >= STARTING_PRICE,
            "Price must never drop below starting price under concurrency");
    }
 
    @Test
    void concurrentBidsSamePriceExactlyOneSucceeds() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
 
        Thread t1 = new Thread(() -> {
            try {
                latch.await();
                auction.processNewBid(bidder1, BID_1);
                successCount.incrementAndGet();
            } catch (InvalidBidException | AuctionClosedException ignored) {
            } catch (Exception e) {
                successCount.incrementAndGet();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                auction.processNewBid(bidder2, BID_1);
                successCount.incrementAndGet();
            } catch (InvalidBidException | AuctionClosedException ignored) {
            } catch (Exception e) {
                successCount.incrementAndGet();
            }
        });
 
        t1.start(); t2.start();
        latch.countDown();
        t1.join(); t2.join();
 
        assertEquals(1, successCount.get(),
            "Exactly one bid must succeed when two threads bid same price");
    }
 
    @Test
    void concurrentBidsNoUnexpectedExceptions() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger unexpectedErrors = new AtomicInteger(0);
 
        Thread t1 = new Thread(() -> {
            try {
                latch.await();
                auction.processNewBid(bidder1, BID_1);
            } catch (InvalidBidException | AuctionClosedException ignored) {
            } catch (Exception e) {
                unexpectedErrors.incrementAndGet();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                auction.processNewBid(bidder2, BID_2);
            } catch (InvalidBidException | AuctionClosedException ignored) {
            } catch (Exception e) {
                unexpectedErrors.incrementAndGet();
            }
        });
 
        t1.start(); t2.start();
        latch.countDown();
        t1.join(); t2.join();
 
        assertEquals(0, unexpectedErrors.get(),
            "No unexpected exceptions must occur during concurrent bidding");
    }
 
    @Test
    void fiveConcurrentBidsPriceIsConsistent() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        double[] prices = {BID_1, BID_2, BID_3, BID_4, BID_5};
        Thread[] threads = new Thread[5];
 
        for (int i = 0; i < 5; i++) {
            final double price = prices[i];
            final Bidder bidder = (i % 2 == 0) ? bidder1 : bidder2;
            threads[i] = new Thread(() -> {
                try { latch.await(); auction.processNewBid(bidder, price); }
                catch (Exception ignored) {}
            });
        }
 
        for (Thread t : threads) t.start();
        latch.countDown();
        for (Thread t : threads) t.join();
 
        assertTrue(auction.getCurrentPrice() >= STARTING_PRICE,
            "Price must be consistent after 5 concurrent bids");
        assertTrue(auction.getBidHistory().size() >= 1,
            "At least one bid must be recorded");
    }
 
    @Test
    void concurrentBidAndCloseNoDeadlock() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
 
        Thread bidThread = new Thread(() -> {
            try { latch.await(); auction.processNewBid(bidder1, BID_1); }
            catch (Exception ignored) {}
        });
        Thread closeThread = new Thread(() -> {
            try { latch.await(); auction.closeAuction(); }
            catch (Exception ignored) {}
        });
 
        bidThread.start(); closeThread.start();
        latch.countDown();
        bidThread.join(2000);
        closeThread.join(2000);
 
        assertFalse(bidThread.isAlive(), "Bid thread must complete without deadlock");
        assertFalse(closeThread.isAlive(), "Close thread must complete without deadlock");
    }
}