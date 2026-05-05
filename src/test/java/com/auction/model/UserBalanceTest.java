package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test đầy đủ các nhánh logic của addBalance và deductBalance trong User.
 * Đây là logic tài chính quan trọng, mỗi nhánh đều cần được kiểm tra.
 */
public class UserBalanceTest {

    private Bidder bidder;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("testuser", "pw");
    }

    // --- addBalance ---

    @Test
    void addBalancePositiveAmountIncreasesBalance() {
        bidder.addBalance(100_000.0);
        assertEquals(100_000.0, bidder.getBalance(), 0.001);
    }

    @Test
    void addBalanceCalledTwiceAccumulates() {
        bidder.addBalance(50_000.0);
        bidder.addBalance(50_000.0);
        assertEquals(100_000.0, bidder.getBalance(), 0.001);
    }

    @Test
    void addBalanceZeroDoesNotChangeBalance() {
        bidder.addBalance(100_000.0);
        bidder.addBalance(0.0);
        assertEquals(100_000.0, bidder.getBalance(), 0.001);
    }

    @Test
    void addBalanceNegativeAmountDoesNotChangeBalance() {
        bidder.addBalance(100_000.0);
        double before = bidder.getBalance();
        bidder.addBalance(-50_000.0);
        assertEquals(before, bidder.getBalance(), 0.001);
    }

    @Test
    void addBalanceNaNDoesNotChangeBalance() {
        bidder.addBalance(100_000.0);
        double before = bidder.getBalance();
        bidder.addBalance(Double.NaN);
        assertEquals(before, bidder.getBalance(), 0.001);
    }

    @Test
    void addBalanceInfinityDoesNotChangeBalance() {
        bidder.addBalance(100_000.0);
        double before = bidder.getBalance();
        bidder.addBalance(Double.POSITIVE_INFINITY);
        assertEquals(before, bidder.getBalance(), 0.001);
    }

    // --- deductBalance ---

    @Test
    void deductBalanceSufficientFundsReturnsTrueAndDeducts() {
        bidder.addBalance(200_000.0);
        boolean result = bidder.deductBalance(100_000.0);
        assertTrue(result);
        assertEquals(100_000.0, bidder.getBalance(), 0.001);
    }

    @Test
    void deductBalanceExactAmountReturnsTrueAndZeroBalance() {
        bidder.addBalance(100_000.0);
        boolean result = bidder.deductBalance(100_000.0);
        assertTrue(result);
        assertEquals(0.0, bidder.getBalance(), 0.001);
    }

    @Test
    void deductBalanceInsufficientFundsReturnsFalse() {
        bidder.addBalance(50_000.0);
        boolean result = bidder.deductBalance(100_000.0);
        assertFalse(result);
    }

    @Test
    void deductBalanceInsufficientFundsDoesNotChangeBalance() {
        bidder.addBalance(50_000.0);
        bidder.deductBalance(100_000.0);
        assertEquals(50_000.0, bidder.getBalance(), 0.001);
    }

    @Test
    void deductBalanceZeroAmountReturnsFalse() {
        bidder.addBalance(100_000.0);
        assertFalse(bidder.deductBalance(0.0));
    }

    @Test
    void deductBalanceNegativeAmountReturnsFalse() {
        bidder.addBalance(100_000.0);
        assertFalse(bidder.deductBalance(-1.0));
    }

    @Test
    void deductBalanceNaNReturnsFalse() {
        bidder.addBalance(100_000.0);
        assertFalse(bidder.deductBalance(Double.NaN));
    }

    @Test
    void deductBalanceInfinityReturnsFalse() {
        bidder.addBalance(100_000.0);
        assertFalse(bidder.deductBalance(Double.POSITIVE_INFINITY));
    }

    @Test
    void deductBalanceFromZeroBalanceReturnsFalse() {
        // balance = 0 ngay từ đầu
        assertFalse(bidder.deductBalance(1.0));
    }

    // --- getBalance thread-safe check ---

    @Test
    void initialBalanceIsZero() {
        assertEquals(0.0, new Bidder("fresh", "pw").getBalance(), 0.001);
    }
}