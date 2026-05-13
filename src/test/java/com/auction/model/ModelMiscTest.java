package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.entities.BidTransaction;
import com.auction.model.entities.item.Art;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.item.Vehicle;
import com.auction.model.entities.user.Bidder;
import com.auction.model.auctionhelpers.AuctionHelperFactory;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Tests for:
 * - User.addBalance / deductBalance edge cases (NaN, Infinite, 0, negative, overflow)
 * - User.addBalance / deductBalance thread-safety
 * - BidTransaction getters/setters/toString
 * - Item subclasses (Art, Electronics, Vehicle) basic props + setCurrentPrice/setHighestBidder
 * - AuctionHelperFactory singleton + creates non-null helpers
 */
public class ModelMiscTest {

    // ==================== User Balance ====================

    @Test
    void addBalanceNaNIsIgnored() {
        Bidder b = new Bidder("u1", "h", null);
        b.addBalance(Double.NaN);
        assertEquals(0.0, b.getBalance(), 0.001);
    }

    @Test
    void addBalanceInfiniteIsIgnored() {
        Bidder b = new Bidder("u2", "h", null);
        b.addBalance(Double.POSITIVE_INFINITY);
        assertEquals(0.0, b.getBalance(), 0.001);
    }

    @Test
    void addBalanceZeroIsIgnored() {
        Bidder b = new Bidder("u3", "h", null);
        b.addBalance(1_000_000.0);
        b.addBalance(0.0);
        assertEquals(1_000_000.0, b.getBalance(), 0.001);
    }

    @Test
    void addBalanceNegativeIsIgnored() {
        Bidder b = new Bidder("u4", "h", null);
        b.addBalance(-500.0);
        assertEquals(0.0, b.getBalance(), 0.001);
    }

    @Test
    void addBalanceOverflowCapsAtMaxSafe() {
        Bidder b = new Bidder("u5", "h", null);
        b.addBalance(Double.MAX_VALUE / 2);
        b.addBalance(Double.MAX_VALUE / 2); // would overflow
        assertTrue(b.getBalance() <= Double.MAX_VALUE / 2 + 1,
            "Balance must not exceed safe ceiling");
    }

    @Test
    void deductBalanceNaNReturnsFalse() {
        Bidder b = new Bidder("u6", "h", null);
        b.addBalance(1_000_000.0);
        assertFalse(b.deductBalance(Double.NaN));
    }

    @Test
    void deductBalanceInfiniteReturnsFalse() {
        Bidder b = new Bidder("u7", "h", null);
        b.addBalance(1_000_000.0);
        assertFalse(b.deductBalance(Double.POSITIVE_INFINITY));
    }

    @Test
    void deductBalanceZeroReturnsFalse() {
        Bidder b = new Bidder("u8", "h", null);
        b.addBalance(1_000_000.0);
        assertFalse(b.deductBalance(0.0));
    }

    @Test
    void deductBalanceNegativeReturnsFalse() {
        Bidder b = new Bidder("u9", "h", null);
        b.addBalance(1_000_000.0);
        assertFalse(b.deductBalance(-100.0));
    }

    @Test
    void deductBalanceExactAmountSucceeds() {
        Bidder b = new Bidder("u10", "h", null);
        b.addBalance(500_000.0);
        assertTrue(b.deductBalance(500_000.0));
        assertEquals(0.0, b.getBalance(), 0.001);
    }

    @Test
    void deductBalanceMoreThanAvailableReturnsFalse() {
        Bidder b = new Bidder("u11", "h", null);
        b.addBalance(100_000.0);
        assertFalse(b.deductBalance(200_000.0));
        assertEquals(100_000.0, b.getBalance(), 0.001, "Balance must not change on failed deduct");
    }

    // ===== Thread-safety =====

    @Test
    void concurrentAddBalanceIsThreadSafe() throws InterruptedException {
        Bidder b = new Bidder("u-thread1", "h", null);
        int threads = 50;
        CountDownLatch latch = new CountDownLatch(1);
        Thread[] ts = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                try { latch.await(); } catch (InterruptedException ignored) {}
                b.addBalance(1_000.0);
            });
            ts[i].start();
        }
        latch.countDown();
        for (Thread t : ts) t.join();

        assertEquals(threads * 1_000.0, b.getBalance(), 0.001,
            "Concurrent addBalance must be thread-safe");
    }

    @Test
    void concurrentDeductBalanceNoNegativeBalance() throws InterruptedException {
        Bidder b = new Bidder("u-thread2", "h", null);
        b.addBalance(100_000.0);

        int threads = 20;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        Thread[] ts = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                try { latch.await(); } catch (InterruptedException ignored) {}
                if (b.deductBalance(10_000.0)) successCount.incrementAndGet();
            });
            ts[i].start();
        }
        latch.countDown();
        for (Thread t : ts) t.join();

        assertTrue(b.getBalance() >= 0, "Balance must never go negative");
        assertEquals(successCount.get() * 10_000.0,
            100_000.0 - b.getBalance(), 0.001,
            "Only deductions that succeeded must reduce balance");
    }

    // ==================== BidTransaction ====================

    @Test
    void bidTransactionGetAmount() {
        Bidder b = new Bidder("bt1", "h", null);
        BidTransaction tx = new BidTransaction(b, 1_000_000.0);
        assertEquals(1_000_000.0, tx.getAmount(), 0.001);
    }

    @Test
    void bidTransactionGetBidder() {
        Bidder b = new Bidder("bt2", "h", null);
        BidTransaction tx = new BidTransaction(b, 500_000.0);
        assertSame(b, tx.getBidder());
    }

    @Test
    void bidTransactionTimestampIsNotNull() {
        Bidder b = new Bidder("bt3", "h", null);
        BidTransaction tx = new BidTransaction(b, 500_000.0);
        assertNotNull(tx.getTimestamp());
    }

    @Test
    void bidTransactionSetAmount() {
        Bidder b = new Bidder("bt4", "h", null);
        BidTransaction tx = new BidTransaction(b, 500_000.0);
        tx.setAmount(999_999.0);
        assertEquals(999_999.0, tx.getAmount(), 0.001);
    }

    @Test
    void bidTransactionSetBidder() {
        Bidder b1 = new Bidder("bt5a", "h", null);
        Bidder b2 = new Bidder("bt5b", "h", null);
        BidTransaction tx = new BidTransaction(b1, 500_000.0);
        tx.setBidder(b2);
        assertSame(b2, tx.getBidder());
    }

    @Test
    void bidTransactionSetTimestamp() {
        Bidder b = new Bidder("bt6", "h", null);
        BidTransaction tx = new BidTransaction(b, 500_000.0);
        LocalDateTime custom = LocalDateTime.of(2025, 1, 1, 12, 0);
        tx.setTimestamp(custom);
        assertEquals(custom, tx.getTimestamp());
    }

    @Test
    void bidTransactionToStringContainsBidderAndAmount() {
        Bidder b = new Bidder("bt7", "h", null);
        BidTransaction tx = new BidTransaction(b, 1_234_567.0);
        String s = tx.toString();
        assertTrue(s.contains("bt7"), "toString must contain bidder username");
        assertTrue(s.contains("1234567") || s.contains("1,234,567") || s.contains("1.234.567"),
            "toString must contain amount");
    }

    // ==================== Item subclasses ====================

    @Test
    void artItemCreation() {
        Art art = new Art("art1", "Mona Lisa", 5_000_000.0);
        assertEquals("Mona Lisa", art.getItemName());
        assertEquals(5_000_000.0, art.getStartingPrice(), 0.001);
    }

    @Test
    void electronicsSetCurrentPrice() {
        Electronics e = new Electronics("el1", "TV", 1_000_000.0);
        e.setCurrentPrice(2_000_000.0);
        assertEquals(2_000_000.0, e.getCurrentPrice(), 0.001);
    }

    @Test
    void vehicleSetHighestBidder() {
        Vehicle v = new Vehicle("v1", "Car", 100_000_000.0);
        v.setHighestBidder("alice");
        assertEquals("alice", v.getHighestBidder());
    }

    @Test
    void itemDefaultHighestBidderIsNoBids() {
        Electronics e = new Electronics("el2", "Phone", 500_000.0);
        assertEquals("No bids yet", e.getHighestBidder());
    }

    // ==================== AuctionHelperFactory ====================

    @Test
    void helperFactorySingletonSameInstance() {
        assertSame(AuctionHelperFactory.getInstance(), AuctionHelperFactory.getInstance());
    }

    @Test
    void helperFactoryCreatesValidator() {
        assertNotNull(AuctionHelperFactory.getInstance().createValidator());
    }

    @Test
    void helperFactoryCreatesNotifier() {
        assertNotNull(AuctionHelperFactory.getInstance().createNotifier());
    }

    @Test
    void helperFactoryCreatesAutoBidProcessor() {
        assertNotNull(AuctionHelperFactory.getInstance().createAutoBidProcessor());
    }

    @Test
    void helperFactoryCreatesFinancialProcessor() {
        assertNotNull(AuctionHelperFactory.getInstance().createFinancialProcessor());
    }

    @Test
    void helperFactoryCreatesSnipingProcessor() {
        assertNotNull(AuctionHelperFactory.getInstance().createSnipingProcessor());
    }
}