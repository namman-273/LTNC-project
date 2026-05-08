package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers missed branches in User:
 * - balanceLock null re-init (after deserialization)
 * - addBalance cap at Double.MAX_VALUE / 2
 * - addBalance NaN/Infinite/negative guards
 * - deductBalance NaN/Infinite/negative guards
 * - checkPassword
 * - toString, getRole, displayInfo
 */
public class UserEdgeCaseTest {

    private Bidder user;

    @BeforeEach
    void setUp() {
        user = new Bidder("testuser", "hashedpw");
        user.addBalance(1_000_000.0);
    }

    // --- balanceLock null re-init (simulate post-deserialization) ---

    @Test
    void addBalanceWorksAfterBalanceLockSetToNull() throws Exception {
        Field lockField = User.class.getDeclaredField("balanceLock");
        lockField.setAccessible(true);
        lockField.set(user, null); // simulate deserialization

        double before = user.getBalance();
        user.addBalance(50000.0);
        assertEquals(before + 50000.0, user.getBalance(), 0.001,
            "addBalance must work after balanceLock is null (re-init path)");
    }

    @Test
    void deductBalanceWorksAfterBalanceLockSetToNull() throws Exception {
        Field lockField = User.class.getDeclaredField("balanceLock");
        lockField.setAccessible(true);
        lockField.set(user, null);

        double before = user.getBalance();
        boolean result = user.deductBalance(50000.0);
        assertTrue(result);
        assertEquals(before - 50000.0, user.getBalance(), 0.001);
    }

    // --- addBalance: balance cap at Double.MAX_VALUE / 2 ---

    @Test
    void addBalanceCapsAtMaxValueHalf() {
        // Set balance near cap via reflection
        try {
            Field balField = User.class.getDeclaredField("balance");
            balField.setAccessible(true);
            balField.set(user, Double.MAX_VALUE / 2 - 1.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        user.addBalance(1_000_000.0); // would overflow
        assertEquals(Double.MAX_VALUE / 2, user.getBalance(), 1.0,
            "Balance must be capped at Double.MAX_VALUE / 2");
    }

    // --- addBalance guards ---

    @Test
    void addBalanceNaNIsIgnored() {
        double before = user.getBalance();
        user.addBalance(Double.NaN);
        assertEquals(before, user.getBalance(), 0.001);
    }

    @Test
    void addBalanceInfiniteIsIgnored() {
        double before = user.getBalance();
        user.addBalance(Double.POSITIVE_INFINITY);
        assertEquals(before, user.getBalance(), 0.001);
    }

    @Test
    void addBalanceNegativeIsIgnored() {
        double before = user.getBalance();
        user.addBalance(-500.0);
        assertEquals(before, user.getBalance(), 0.001);
    }

    @Test
    void addBalanceZeroIsIgnored() {
        double before = user.getBalance();
        user.addBalance(0.0);
        assertEquals(before, user.getBalance(), 0.001);
    }

    // --- deductBalance guards ---

    @Test
    void deductBalanceNaNReturnsFalse() {
        assertFalse(user.deductBalance(Double.NaN));
    }

    @Test
    void deductBalanceInfiniteReturnsFalse() {
        assertFalse(user.deductBalance(Double.POSITIVE_INFINITY));
    }

    @Test
    void deductBalanceNegativeReturnsFalse() {
        assertFalse(user.deductBalance(-100.0));
    }

    @Test
    void deductBalanceZeroReturnsFalse() {
        assertFalse(user.deductBalance(0.0));
    }

    @Test
    void deductBalanceInsufficientReturnsFalse() {
        assertFalse(user.deductBalance(user.getBalance() + 1.0));
    }

    @Test
    void deductBalanceExactAmountSucceeds() {
        double balance = user.getBalance();
        assertTrue(user.deductBalance(balance));
        assertEquals(0.0, user.getBalance(), 0.001);
    }

    // --- checkPassword ---

    @Test
    void checkPasswordCorrectReturnsTrue() {
        Bidder b = new Bidder("alice", com.auction.util.SecurityUtils.hashPassword("secret", "alice"));
        assertTrue(b.checkPassword("secret"));
    }

    @Test
    void checkPasswordWrongReturnsFalse() {
        Bidder b = new Bidder("alice", com.auction.util.SecurityUtils.hashPassword("secret", "alice"));
        assertFalse(b.checkPassword("wrong"));
    }

    // --- toString, getRole ---

    @Test
    void toStringContainsUsername() {
        assertTrue(user.toString().contains("testuser"));
    }

    @Test
    void getRoleReturnsBidder() {
        assertEquals("BIDDER", user.getRole());
    }

    // --- Admin / Seller displayInfo ---

    @Test
    void adminDisplayInfoDoesNotThrow() {
        Admin admin = new Admin("admin1", "pw");
        admin.displayInfo(); // just verify no exception
    }

    @Test
    void sellerDisplayInfoDoesNotThrow() {
        Seller seller = new Seller("seller1", "pw");
        seller.displayInfo();
    }

    @Test
    void bidderDisplayInfoDoesNotThrow() {
        user.displayInfo();
    }

    // --- Entity.setId ---

    @Test
    void entitySetIdUpdatesId() {
        user.setId("new-id");
        assertEquals("new-id", user.getId());
    }

    // --- update (Observer implementation) ---

    @Test
    void updateDoesNotThrow() {
        user.update("TEST_MESSAGE");
    }
}