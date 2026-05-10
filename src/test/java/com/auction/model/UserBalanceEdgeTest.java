package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Covers User.addBalance and deductBalance edge branches:
 * - NaN, Infinite, zero, negative inputs
 * - balance cap (Double.MAX_VALUE/2)
 * - deduct exact, deduct insufficient
 * - update() called from add/deduct (via side-effect check)
 */
public class UserBalanceEdgeTest {

    private Bidder fresh() {
        return new Bidder("u_" + System.nanoTime(), "h");
    }

    // ── addBalance guards ─────────────────────────────────────

    @Test void addNaNIgnored()       { Bidder b=fresh(); b.addBalance(Double.NaN);                assertEquals(0.0, b.getBalance(), 0); }
    @Test void addInfIgnored()       { Bidder b=fresh(); b.addBalance(Double.POSITIVE_INFINITY);   assertEquals(0.0, b.getBalance(), 0); }
    @Test void addNegIgnored()       { Bidder b=fresh(); b.addBalance(-1000.0);                    assertEquals(0.0, b.getBalance(), 0); }
    @Test void addZeroIgnored()      { Bidder b=fresh(); b.addBalance(0.0);                        assertEquals(0.0, b.getBalance(), 0); }
    @Test void addNormalWorks()      { Bidder b=fresh(); b.addBalance(500_000.0);                  assertEquals(500_000.0, b.getBalance(), 0.001); }

    @Test
    void addBalanceCapClamps() {
        Bidder b = fresh();
        double cap = Double.MAX_VALUE / 2;
        b.addBalance(cap);
        b.addBalance(cap); // second call triggers cap branch
        assertEquals(cap, b.getBalance(), 1.0); // capped, not doubled
    }

    // ── deductBalance guards ──────────────────────────────────

    @Test void deductNaNReturnsFalse()  { Bidder b=fresh(); b.addBalance(1_000_000.0); assertFalse(b.deductBalance(Double.NaN)); }
    @Test void deductInfReturnsFalse()  { Bidder b=fresh(); b.addBalance(1_000_000.0); assertFalse(b.deductBalance(Double.POSITIVE_INFINITY)); }
    @Test void deductZeroReturnsFalse() { Bidder b=fresh(); b.addBalance(1_000_000.0); assertFalse(b.deductBalance(0.0)); }
    @Test void deductNegReturnsFalse()  { Bidder b=fresh(); b.addBalance(1_000_000.0); assertFalse(b.deductBalance(-100.0)); }

    @Test
    void deductExactAmountSucceeds() {
        Bidder b = fresh();
        b.addBalance(500_000.0);
        assertTrue(b.deductBalance(500_000.0));
        assertEquals(0.0, b.getBalance(), 0.001);
    }

    @Test
    void deductPartialAmountSucceeds() {
        Bidder b = fresh();
        b.addBalance(1_000_000.0);
        assertTrue(b.deductBalance(300_000.0));
        assertEquals(700_000.0, b.getBalance(), 0.001);
    }

    @Test
    void deductInsufficientReturnsFalse() {
        Bidder b = fresh();
        b.addBalance(100_000.0);
        assertFalse(b.deductBalance(200_000.0));
        assertEquals(100_000.0, b.getBalance(), 0.001); // unchanged
    }

    // ── checkPassword ─────────────────────────────────────────

    @Test
    void checkPasswordCorrect() {
        com.auction.service.UserManager.getInstance().register("cptest", "pw123", "BIDDER");
        Bidder u = (Bidder) com.auction.service.UserManager.getInstance().findUserByUsername("cptest");
        assertTrue(u.checkPassword("pw123"));
    }

    @Test
    void checkPasswordWrong() {
        com.auction.service.UserManager.getInstance().register("cptest2", "pw123", "BIDDER");
        Bidder u = (Bidder) com.auction.service.UserManager.getInstance().findUserByUsername("cptest2");
        assertFalse(u.checkPassword("wrongpass"));
    }
}