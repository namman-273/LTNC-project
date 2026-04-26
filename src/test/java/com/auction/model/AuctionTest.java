package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionTest {
 
  private static final double STARTING_PRICE = 1000.0;
  private static final double VALID_BID_1 = 1200.0;
  private static final double VALID_BID_2 = 1500.0;
  private static final double VALID_BID_3 = 1800.0;
  private static final double LOW_BID = 500.0;
  private static final long DURATION = 9999L;
 
  private Auction auction;
  private Bidder bidder1;
  private Bidder bidder2;
  private Item item;
 
  @BeforeEach
  void setUp() {
    item = new Electronics("item-01", "Laptop", STARTING_PRICE);
    auction = new Auction("auction-01", item, DURATION);
    bidder1 = new Bidder("alice", "pass123");
    bidder2 = new Bidder("bob", "pass456");
    auction.setStatus(AuctionStatus.RUNNING);
  }
 
  // --- processNewBid: hợp lệ ---
 
  @Test
  void bidSuccessUpdatesCurrentPrice() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, item.getCurrentPrice());
  }
 
  @Test
  void bidSuccessAddsToHistory() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(1, auction.getBidHistory().size());
  }
 
  @Test
  void bidSuccessHistoryRecordsCorrectAmount() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, auction.getBidHistory().get(0).getAmount());
  }
 
  @Test
  void bidSuccessMultipleBidsHistoryHasTwo() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    auction.processNewBid(bidder2, VALID_BID_2);
    assertEquals(2, auction.getBidHistory().size());
  }
 
  @Test
  void bidSuccessMultipleBidsPriceUpdatesToHighest() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    auction.processNewBid(bidder2, VALID_BID_2);
    assertEquals(VALID_BID_2, item.getCurrentPrice());
  }
 
  // --- processNewBid: lỗi xác thực ---
 
  @Test
  void bidWithNullBidderThrowsAuthenticationException() {
    assertThrows(
        AuthenticationException.class,
        () -> auction.processNewBid(null, VALID_BID_1));
  }
 
  // --- processNewBid: lỗi giá ---
 
  @Test
  void bidEqualToStartingPriceThrowsInvalidBidException() {
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, STARTING_PRICE));
  }
 
  @Test
  void bidLowerThanStartingPriceThrowsInvalidBidException() {
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, LOW_BID));
  }
 
  @Test
  void bidLowerAfterFirstBidThrowsInvalidBidException() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_2);
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder2, VALID_BID_1));
  }
 
  // --- processNewBid: phiên đóng ---
 
  @Test
  void bidWhenFinishedThrowsAuctionClosedException() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, VALID_BID_1));
  }
 
  @Test
  void bidWhenPaidThrowsAuctionClosedException() {
    auction.setStatus(AuctionStatus.PAID);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, VALID_BID_1));
  }
 
  @Test
  void bidWhenCanceledThrowsAuctionClosedException() {
    auction.setStatus(AuctionStatus.CANCELED);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, VALID_BID_1));
  }
 
  // --- Observer ---
 
  @Test
  void addObserverReceivesNotificationOnBid() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.processNewBid(bidder1, VALID_BID_1);
    assertTrue(received.size() >= 1);
  }
 
  @Test
  void removeObserverDoesNotReceiveNotification() throws Exception {
    List<String> received = new ArrayList<>();
    Observer obs = msg -> received.add(msg);
    auction.addObserver(obs);
    auction.removeObserver(obs);
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(0, received.size());
  }
 
  @Test
  void notifyObserversDoesNotThrow() {
    assertDoesNotThrow(() -> auction.notifyObservers("test-msg"));
  }
 
  // --- Getter / Setter ---
 
  @Test
  void getStatusAfterSetRunningReturnsRunning() {
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());
  }
 
  @Test
  void setStatusToFinishedChangesStatus() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void getItemReturnsItemPassedToConstructor() {
    assertEquals(item, auction.getItem());
  }
 
  @Test
  void getIdReturnsIdPassedToConstructor() {
    assertEquals("auction-01", auction.getId());
  }
 
  @Test
  void restoreTransientsCanBeCalledMultipleTimes() {
    assertDoesNotThrow(() -> {
      auction.restoreTransients();
      auction.restoreTransients();
    });
  }
 
  // --- AutoBid ---
 
  @Test
  void addAutoBidConfigDoesNotThrow() {
    assertDoesNotThrow(() ->
        auction.addAutoBidConfig("alice", VALID_BID_3, 100.0));
  }
 
  @Test
  void addAutoBidConfigTriggersAutoBidWhenPriceBelowMax() throws Exception {
    auction.processNewBid(bidder2, VALID_BID_1);
    auction.addAutoBidConfig("alice", VALID_BID_3, 100.0);
    assertTrue(item.getCurrentPrice() > VALID_BID_1);
  }
 
  // --- Concurrency ---
 
  @Test
  void concurrentBidsDoNotCrash() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    Thread thread = new Thread(
        () -> {
          try {
            auction.processNewBid(bidder2, VALID_BID_2);
          } catch (Exception ignored) {
            // intentionally ignored
          }
        });
    thread.start();
    thread.join();
    assertTrue(auction.getBidHistory().size() >= 1);
  }
}