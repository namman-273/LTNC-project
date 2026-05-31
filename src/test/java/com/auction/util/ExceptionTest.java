package com.auction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.user.Bidder;
import com.auction.model.enums.AuctionStatus;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.AuctionClosedException;
import com.auction.util.exception.AuthenticationException;
import com.auction.util.exception.InvalidBidException;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test exception được ném đúng context trong business logic,
 * không chỉ test constructor tồn tại.
 */
public class ExceptionTest {

  private static final double STARTING_PRICE = 1000.0;
  private static final long DURATION = 9999L;

  private Auction auction;
  private Bidder bidder;

  @BeforeEach
  void setUp() throws Exception {
    // Holder idiom: clear users via setUsers() thay vì reflection.
    UserManager.getInstance().setUsers(new java.util.HashMap<>());
    UserManager.getInstance().register("alice", "pw", "BIDDER", "alice@test.com");
    bidder = (Bidder) UserManager.getInstance().findUserByUsername("alice");
    bidder.addBalance(10_000_000.0);

    auction = new Auction("exc-1", new Electronics("e-exc", "TV", STARTING_PRICE), DURATION, null);
    auction.setStatus(AuctionStatus.RUNNING);
  }

  // --- InvalidBidException: thrown khi giá quá thấp ---

  @Test
  void processNewBidBelowMinimumIncrementThrowsInvalidBidException() {
    InvalidBidException ex = assertThrows(InvalidBidException.class,
        () -> auction.processNewBid(bidder, STARTING_PRICE + 1.0));
    assertEquals(InvalidBidException.class, ex.getClass());
  }

  @Test
  void processNewBidEqualToCurrentPriceThrowsInvalidBidException() {
    assertThrows(InvalidBidException.class,
        () -> auction.processNewBid(bidder, STARTING_PRICE));
  }

  @Test
  void processNewBidNegativeAmountThrowsInvalidBidException() {
    assertThrows(InvalidBidException.class,
        () -> auction.processNewBid(bidder, -500.0));
  }

  @Test
  void processNewBidInsufficientBalanceThrowsInvalidBidException() throws Exception {
    // Bidder mới không có balance
    // Holder idiom: clear users via setUsers() thay vì reflection.
    UserManager.getInstance().setUsers(new java.util.HashMap<>());
    UserManager.getInstance().register("broke", "pw", "BIDDER", "broke@test.com");
    Bidder brokeBidder = (Bidder) UserManager.getInstance().findUserByUsername("broke");
    // balance = 0, không thể bid 51000
    assertThrows(InvalidBidException.class,
        () -> auction.processNewBid(brokeBidder, STARTING_PRICE + 50000.0));
  }

  // --- AuctionClosedException: thrown khi phiên đã đóng ---

  @Test
  void processNewBidOnFinishedAuctionThrowsAuctionClosedException() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertThrows(AuctionClosedException.class,
        () -> auction.processNewBid(bidder, STARTING_PRICE + 50000.0));
  }

  @Test
  void processNewBidOnCanceledAuctionThrowsAuctionClosedException() {
    auction.setStatus(AuctionStatus.CANCELED);
    assertThrows(AuctionClosedException.class,
        () -> auction.processNewBid(bidder, STARTING_PRICE + 50000.0));
  }

  @Test
  void processNewBidOnPaidAuctionThrowsAuctionClosedException() {
    auction.setStatus(AuctionStatus.PAID);
    assertThrows(AuctionClosedException.class,
        () -> auction.processNewBid(bidder, STARTING_PRICE + 50000.0));
  }

  @Test
  void processNewBidOnExpiredAuctionThrowsAuctionClosedException() throws Exception {
    Field endTimeField = Auction.class.getDeclaredField("endTime");
    endTimeField.setAccessible(true);
    endTimeField.set(auction, System.currentTimeMillis() - 1000L);
    assertThrows(AuctionClosedException.class,
        () -> auction.processNewBid(bidder, STARTING_PRICE + 50000.0));
  }

  // --- AuthenticationException: thrown khi bidder null ---

  @Test
  void processNewBidNullBidderThrowsAuthenticationException() {
    assertThrows(AuthenticationException.class,
        () -> auction.processNewBid(null, STARTING_PRICE + 50000.0));
  }

  // --- InvalidBidException: seller tự bid ---

  @Test
  void sellerBiddingOwnAuctionThrowsInvalidBidException() throws Exception {
    // Holder idiom: clear users via setUsers() thay vì reflection.
    UserManager.getInstance().setUsers(new java.util.HashMap<>());
    UserManager.getInstance().register("seller1", "pw", "BIDDER", "seller1@test.com");
    Bidder seller = (Bidder) UserManager.getInstance().findUserByUsername("seller1");
    seller.addBalance(10_000_000.0);

    // Tạo auction có sellerId = "seller1"
    Auction ownAuction = new Auction("own-1",
        new Electronics("e-own", "Laptop", STARTING_PRICE), DURATION, "seller1");
    ownAuction.setStatus(AuctionStatus.RUNNING);

    assertThrows(InvalidBidException.class,
        () -> ownAuction.processNewBid(seller, STARTING_PRICE + 50000.0));
  }
}