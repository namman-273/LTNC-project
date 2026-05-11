package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.user.Bidder;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.helpers.AuctionSnipingProcessor;
import com.auction.service.UserManager;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test AuctionSnipingProcessor.handleAntiSniping:
 * - timeLeft > oneMinute → không gia hạn
 * - timeLeft <= 0 → không gia hạn
 * - timeLeft trong khoảng [0, oneMinute) và ext < max → gia hạn
 * - đã đạt max extensions → không gia hạn
 */
public class AuctionSnipingProcessorTest {

    private static final long ONE_MIN   = 60_000L;
    private static final long TWO_MIN   = 120_000L;
    private static final int  MAX_EXT   = 3;
    private static final long DURATION  = 9999L;

    private AuctionSnipingProcessor processor;
    private Bidder bidder;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        UserManager.getInstance().register("alice", "pw", "BIDDER");
        bidder = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder.addBalance(50_000_000.0);

        processor = new AuctionSnipingProcessor();
    }

    // ===== Không gia hạn =====

    @Test
    void noExtensionWhenMoreThanOneMinuteLeft() {
        Auction a = auctionWithTimeLeft(120_000L); // 2 phút còn lại
        AtomicLong extended = new AtomicLong(0);

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, 0,
            extra -> extended.set(extra));

        assertEquals(0, extended.get(), "Should not extend when > 1 minute left");
    }

    @Test
    void noExtensionWhenTimeAlreadyExpired() {
        Auction a = auctionWithTimeLeft(-5_000L); // đã hết giờ
        AtomicLong extended = new AtomicLong(0);

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, 0,
            extra -> extended.set(extra));

        assertEquals(0, extended.get(), "Should not extend when time has expired");
    }

    @Test
    void noExtensionWhenMaxExtensionsReached() {
        Auction a = auctionWithTimeLeft(30_000L); // 30 giây còn lại
        AtomicLong extended = new AtomicLong(0);

        // currentExt == MAX_EXT
        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, MAX_EXT,
            extra -> extended.set(extra));

        assertEquals(0, extended.get(), "Should not extend when max extensions reached");
    }

    // ===== Gia hạn =====

    @Test
    void extensionAppliedWhenWithinLastMinute() {
        Auction a = auctionWithTimeLeft(30_000L); // 30 giây còn lại
        AtomicLong extended = new AtomicLong(0);

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, 0,
            extra -> extended.set(extra));

        assertEquals(TWO_MIN, extended.get(), "Should extend by TWO_MIN when sniping detected");
    }

    @Test
    void extensionAppliedWhenJustBeforeOneMinute() {
        Auction a = auctionWithTimeLeft(ONE_MIN - 1); // 1ms trước ngưỡng
        AtomicLong extended = new AtomicLong(0);

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, 0,
            extra -> extended.set(extra));

        assertEquals(TWO_MIN, extended.get());
    }

    @Test
    void extensionAppliedOnSecondSniping() {
        Auction a = auctionWithTimeLeft(30_000L);
        AtomicInteger callCount = new AtomicInteger(0);

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, 1,
            extra -> callCount.incrementAndGet());

        assertEquals(1, callCount.get(), "Should extend on 2nd sniping (ext=1 < max=3)");
    }

    @Test
    void extensionNotAppliedOnThirdSniping() {
        Auction a = auctionWithTimeLeft(30_000L);
        AtomicInteger callCount = new AtomicInteger(0);

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, MAX_EXT,
            extra -> callCount.incrementAndGet());

        assertEquals(0, callCount.get(), "Should NOT extend once max extensions reached");
    }

    @Test
    void extensionIsExactlyTwoMinutes() {
        Auction a = auctionWithTimeLeft(10_000L); // 10 giây
        final long[] captured = {0};

        processor.handleAntiSniping(a, bidder, ONE_MIN, TWO_MIN, MAX_EXT, 0,
            extra -> captured[0] = extra);

        assertEquals(TWO_MIN, captured[0], 0.001, "Extension must be exactly 2 minutes");
    }

    // ===== helper =====

    private Auction auctionWithTimeLeft(long timeLeftMs) {
        long endTime = System.currentTimeMillis() + timeLeftMs;
        Auction a = new Auction("SNIPE-001",
            new Electronics("e-snipe", "Item", 500_000.0),
            DURATION, "seller");
        a.setStatus(AuctionStatus.RUNNING);
        try {
            Field f = Auction.class.getDeclaredField("endTime");
            f.setAccessible(true);
            f.set(a, endTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return a;
    }
}