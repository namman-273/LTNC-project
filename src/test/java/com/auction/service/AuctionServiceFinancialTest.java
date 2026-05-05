package com.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Seller;


public class AuctionServiceFinancialTest {

    private static final long DURATION = 9999L;
    private static final double STARTING_PRICE = 500_000.0;
    private static final double BID_1 = 550_000.0;
    private static final double BID_2 = 600_000.0;

    private AuctionService service;

    @BeforeEach
    void setUp() throws Exception {
        Field asField = AuctionService.class.getDeclaredField("instance");
        asField.setAccessible(true);
        asField.set(null, null);

        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        service = AuctionService.getInstance();
        UserManager.getInstance().register("seller1", "pw", "SELLER");
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        UserManager.getInstance().register("bob", "pw", "BIDDER");
    }

    // --- Seller nhận tiền khi auction kết thúc ---

    @Test
    void endAuctionWithWinnerTransfersFundsToSeller() throws Exception {
        Seller seller = (Seller) UserManager.getInstance().findUserByUsername("seller1");
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        alice.addBalance(10_000_000.0);

        Auction a = new Auction("fin-1",
            new Electronics("e-fin1", "TV", STARTING_PRICE), DURATION, "seller1");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        a.processNewBid(alice, BID_1);
        double sellerBefore = seller.getBalance();

        service.endAuction("fin-1");

        assertEquals(sellerBefore + BID_1, seller.getBalance(), 0.001,
            "Seller must receive the winning bid amount");
    }

    
    @Test
    void endAuctionNoBidsSetStatusToFinishedNotPaid() {
        Auction a = new Auction("fin-3",
            new Electronics("e-fin3", "Camera", STARTING_PRICE), DURATION, "seller1");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        service.endAuction("fin-3");

        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    @Test
    void endAuctionNoBidsSellerReceivesNoMoney() {
        Seller seller = (Seller) UserManager.getInstance().findUserByUsername("seller1");
        double before = seller.getBalance();

        Auction a = new Auction("fin-4",
            new Electronics("e-fin4", "Watch", STARTING_PRICE), DURATION, "seller1");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        service.endAuction("fin-4");

        assertEquals(before, seller.getBalance(), 0.001,
            "Seller must not receive money when no bids placed");
    }

    @Test
    void endAuctionLastBidderIsWinner() throws Exception {
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        Bidder bob = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        alice.addBalance(10_000_000.0);
        bob.addBalance(10_000_000.0);

        Auction a = new Auction("fin-5",
            new Electronics("e-fin5", "Laptop", STARTING_PRICE), DURATION, "seller1");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        a.processNewBid(alice, BID_1);
        a.processNewBid(bob, BID_2);

        final String[] notifyMsg = {""};
        a.addObserver(msg -> notifyMsg[0] = msg);

        service.endAuction("fin-5");
        Thread.sleep(100);

        // Bob bid sau cùng nên là winner
        assertTrue(notifyMsg[0].contains("bob"),
            "Last bidder (bob) must be declared winner");
    }

    @Test
    void endAuctionWinnerBidAmountInNotification() throws Exception {
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        alice.addBalance(10_000_000.0);

        Auction a = new Auction("fin-6",
            new Electronics("e-fin6", "Phone", STARTING_PRICE), DURATION, "seller1");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        a.processNewBid(alice, BID_1);

        final String[] notifyMsg = {""};
        a.addObserver(msg -> notifyMsg[0] = msg);

        service.endAuction("fin-6");
        Thread.sleep(100);

        assertTrue(notifyMsg[0].contains(String.valueOf(BID_1)),
            "Notification must contain the winning bid amount");
    }

    @Test
    void endAuctionNoBidsNotificationContainsNoWinner() throws Exception {
        Auction a = new Auction("fin-7",
            new Electronics("e-fin7", "Tablet", STARTING_PRICE), DURATION, "seller1");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        final String[] notifyMsg = {""};
        a.addObserver(msg -> notifyMsg[0] = msg);

        service.endAuction("fin-7");
        Thread.sleep(100);

        assertTrue(notifyMsg[0].contains("No winner"),
            "Notification must say 'No winner' when no bids");
    }

    // --- Null/không tìm thấy seller ---

    @Test
    void endAuctionSellerNotFoundDoesNotCrashAndStatusFinished() throws Exception {
        Bidder alice = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        alice.addBalance(10_000_000.0);

        // sellerId = "unknown-seller" không có trong UserManager
        Auction a = new Auction("fin-8",
            new Electronics("e-fin8", "Monitor", STARTING_PRICE), DURATION, "unknown-seller");
        a.setStatus(AuctionStatus.RUNNING);
        service.addAuction(a);

        a.processNewBid(alice, BID_1);
        service.endAuction("fin-8");

        // Seller null → status stays at FINISHED (không PAID)
        assertEquals(AuctionStatus.FINISHED, a.getStatus());
    }

    // --- getWatchlistForUser ---

    @Test
    void getWatchlistForUserNullUsernameReturnsEmpty() {
        assertTrue(service.getWatchlistForUser("nonexistent").isEmpty());
    }
}