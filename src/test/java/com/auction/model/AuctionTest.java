package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuctionTest {

  private static final double STARTING_PRICE = 1000.0;
  private static final double VALID_BID = 1200.0;
  private static final double HIGH_BID = 1500.0;
  private static final double HIGHER_BID = 1800.0;
  private static final double LOW_BID = 500.0;

  private Auction auction;
  private Bidder bidder1;
  private Bidder bidder2;
  private Item item;

  @BeforeEach
  void setUp() {
    item = new Electronics("item-01", "Laptop", STARTING_PRICE);
    auction = new Auction("auction-01", item);
    bidder1 = new Bidder("alice", "pass123");
    bidder2 = new Bidder("bob", "pass456");
  }

  // --- processNewBid: hợp lệ ---

  @Test
  void testBidSuccess_updatesCurrentPrice() throws Exception {
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, HIGH_BID);
    assertEquals(HIGH_BID, item.getCurrentPrice());
  }

  @Test
  void testBidSuccess_addsToHistory() throws Exception {
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, VALID_BID);
    assertEquals(1, auction.getBidHistory().size());
    assertEquals(VALID_BID, auction.getBidHistory().get(0).getAmount());
  }

  @Test
  void testBidSuccess_multipleBids() throws Exception {
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, VALID_BID);
    auction.processNewBid(bidder2, HIGH_BID);
    assertEquals(2, auction.getBidHistory().size());
    assertEquals(HIGH_BID, item.getCurrentPrice());
  }

  @Test
  void testBidOnOpenAuction_shouldSucceed() throws Exception {
    auction.processNewBid(bidder1, VALID_BID);
    assertEquals(VALID_BID, item.getCurrentPrice());
  }

  // --- processNewBid: lỗi xác thực ---

  @Test
  void testBid_nullBidder_throwsAuthenticationException() {
    auction.setStatus(AuctionStatus.RUNNING);
    assertThrows(
        AuthenticationException.class,
        () -> auction.processNewBid(null, HIGH_BID));
  }

  // --- processNewBid: lỗi giá thấp ---

  @Test
  void testBid_amountEqualCurrentPrice_throwsInvalidBidException() {
    auction.setStatus(AuctionStatus.RUNNING);
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, STARTING_PRICE));
  }

  @Test
  void testBid_amountLowerThanCurrentPrice_throwsInvalidBidException() {
    auction.setStatus(AuctionStatus.RUNNING);
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, LOW_BID));
  }

  @Test
  void testBid_amountLowerAfterFirstBid_throwsInvalidBidException() throws Exception {
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, HIGH_BID);
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder2, VALID_BID));
  }

  // --- processNewBid: lỗi phiên đóng ---

  @Test
  void testBid_whenFinished_throwsAuctionClosedException() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, HIGH_BID));
  }

  @Test
  void testBid_whenPaid_throwsAuctionClosedException() {
    auction.setStatus(AuctionStatus.PAID);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, HIGH_BID));
  }

  @Test
  void testBid_whenCanceled_throwsAuctionClosedException() {
    auction.setStatus(AuctionStatus.CANCELED);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, HIGH_BID));
  }

  // --- Observer ---

  @Test
  void testAddObserver_receivesNotification() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, VALID_BID);
    assertEquals(1, received.size());
  }

  @Test
  void testRemoveObserver_doesNotReceiveNotification() throws Exception {
    List<String> received = new ArrayList<>();
    Observer obs = msg -> received.add(msg);
    auction.addObserver(obs);
    auction.removeObserver(obs);
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, VALID_BID);
    assertEquals(0, received.size());
  }

  @Test
  void testNotifyObservers_doesNotThrow() {
    assertDoesNotThrow(() -> auction.notifyObservers("test"));
  }

  // --- Getter / Setter ---

  @Test
  void testGetStatus_defaultIsOpen() {
    assertEquals(AuctionStatus.OPEN, auction.getStatus());
  }

  @Test
  void testSetStatus_changesStatus() {
    auction.setStatus(AuctionStatus.RUNNING);
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());
  }

  @Test
  void testGetItem_returnsCorrectItem() {
    assertEquals(item, auction.getItem());
  }

  @Test
  void testGetId_returnsCorrectId() {
    assertEquals("auction-01", auction.getId());
  }

  @Test
  void testRestoreTransients_doesNotThrow() {
    assertDoesNotThrow(() -> auction.restoreTransients());
  }

  // --- Concurrency ---

  @Test
  void testConcurrentBids_onlyHighestWins() throws Exception {
    auction.setStatus(AuctionStatus.RUNNING);
    auction.processNewBid(bidder1, HIGH_BID);

    Thread thread = new Thread(
        () -> {
          try {
            auction.processNewBid(bidder2, HIGHER_BID);
          } catch (Exception ignored) {
            // intentionally ignored
          }
        });
    thread.start();
    thread.join();

    assertEquals(HIGHER_BID, item.getCurrentPrice());
  }
}