package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
 
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionInputValidationTest {
 
    private static final double STARTING_PRICE = 1000.0;
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
        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder2 = (Bidder) UserManager.getInstance().findUserByUsername("bob");
        bidder1.addBalance(10_000_000.0);
        bidder2.addBalance(10_000_000.0);
 
        Item item = new Electronics("item-v", "Laptop", STARTING_PRICE);
        auction = new Auction("auction-v", item, DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
    }
 
    // --- input validation: #7 ---
 
    @Test
    void nullBidderThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class,
            () -> auction.processNewBid(null, STARTING_PRICE + 100.0));
    }
 
    @Test
    void bidNegativeAmountThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder1, -100.0));
    }
 
    @Test
    void bidZeroAmountThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder1, 0.0));
    }
 
    @Test
    void nullItemInConstructorThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> new Auction("null-item", null, DURATION, null));
    }
 
    // --- boundary values: #8 ---
 
    @Test
    void bidJustAboveCurrentPriceSucceeds() throws Exception {
        double justAbove = STARTING_PRICE + 50000.0;
        auction.processNewBid(bidder1, justAbove);
        assertEquals(justAbove, auction.getCurrentPrice(), 0.001,
            "Bid of minimum increment above current price must succeed");
    }
 
    @Test
    void bidExactlyAtCurrentPriceThrows() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder1, STARTING_PRICE));
    }
 
    @Test
    void bidJustBelowCurrentPriceThrows() {
        assertThrows(InvalidBidException.class,
            () -> auction.processNewBid(bidder1, STARTING_PRICE - 0.01));
    }
 
    @Test
    void zeroDurationAuctionIsExpiredImmediately() throws Exception {
        Auction expired = new Auction("zero-dur", new Electronics("ez", "Item", STARTING_PRICE), 1L, null);
        expired.setStatus(AuctionStatus.RUNNING);
        // Dùng reflection set endTime về quá khứ — tránh phụ thuộc timing của CI
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        endTimeField.set(expired, System.currentTimeMillis() - 1);
        assertThrows(AuctionClosedException.class,
            () -> expired.processNewBid(bidder1, STARTING_PRICE + 100.0));
    }
 
    @Test
    void auctionEndTimeInPastThrowsWhenBidding() throws Exception {
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        endTimeField.set(auction, System.currentTimeMillis() - 1000);
 
        assertThrows(AuctionClosedException.class,
            () -> auction.processNewBid(bidder1, STARTING_PRICE + 100.0));
    }
 
    @Test
    void bidExactlyOneIncrementAboveCurrentIsValid() throws Exception {
        auction.processNewBid(bidder1, STARTING_PRICE + 50000.0);
        assertDoesNotThrow(() -> auction.processNewBid(bidder2, STARTING_PRICE + 100000.0));
    }
 
    @Test
    void consecutiveSmallIncrementsAllRecorded() throws Exception {
        auction.processNewBid(bidder1, STARTING_PRICE + 50000.0);
        auction.processNewBid(bidder2, STARTING_PRICE + 100000.0);
        auction.processNewBid(bidder1, STARTING_PRICE + 150000.0);
 
        assertEquals(3, auction.getBidHistory().size(),
            "Three consecutive valid-increment bids must all be recorded");
    }
}