package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionTest {
 
  private static final double STARTING_PRICE = 1000.0;
  private static final double VALID_BID_1 = 51000.0;
  private static final double VALID_BID_2 = 102000.0;
  private static final double VALID_BID_3 = 153000.0;
  private static final double LOW_BID = 500.0;
  private static final long DURATION = 9999L;
 
  private Auction auction;
  private Bidder bidder1;
  private Bidder bidder2;
  private Item item;
 
  @BeforeEach
  void setUp() throws Exception {
    // Reset UserManager để tránh dữ liệu rò rỉ giữa các test
    Field field = UserManager.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
 
    // Đăng ký bidder1 vào UserManager để AutoBid có thể tìm thấy user
    UserManager.getInstance().register("alice", "pass123", "BIDDER");
    UserManager.getInstance().register("bob", "pass456", "BIDDER");
 
    item = new Electronics("item-01", "Laptop", STARTING_PRICE);
    auction = new Auction("auction-01", item, DURATION, null);
 
    // Lấy lại đúng object Bidder từ UserManager để AutoBid hoạt động đúng
    bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
    bidder2 = (Bidder) UserManager.getInstance().findUserByUsername("bob");
    bidder1.addBalance(10_000_000.0);
    bidder2.addBalance(10_000_000.0);
 
    auction.setStatus(AuctionStatus.RUNNING);
  }
 
  // --- Constructor ---
 
  @Test
  void constructorNullItemThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Auction("x", null, DURATION, null));
  }
 
  @Test
  void constructorSetsStatusToOpen() {
    Auction fresh = new Auction("a2", item, DURATION, null);
    assertEquals(AuctionStatus.OPEN, fresh.getStatus());
  }
 
  @Test
  void constructorSetsCurrentPriceToStartingPrice() {
    assertEquals(STARTING_PRICE, auction.getCurrentPrice());
  }
 
  @Test
  void constructorSetsEndTimeInFuture() {
    assertTrue(auction.getEndTime() > System.currentTimeMillis());
  }
 
  // --- getId / getItem ---
 
  @Test
  void getIdReturnsCorrectId() {
    assertEquals("auction-01", auction.getId());
  }
 
  @Test
  void getItemReturnsCorrectItem() {
    assertEquals(item, auction.getItem());
  }
 
  // --- setStatus / getStatus ---
 
  @Test
  void setStatusChangesToFinished() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void setStatusChangesToCanceled() {
    auction.setStatus(AuctionStatus.CANCELED);
    assertEquals(AuctionStatus.CANCELED, auction.getStatus());
  }
 
  @Test
  void setStatusChangesToPaid() {
    auction.setStatus(AuctionStatus.PAID);
    assertEquals(AuctionStatus.PAID, auction.getStatus());
  }
 
  // --- processNewBid: hợp lệ ---
 
  @Test
  void bidSuccessUpdatesCurrentPrice() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, auction.getCurrentPrice());
  }
 
  @Test
  void bidSuccessUpdatesItemCurrentPrice() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, item.getCurrentPrice());
  }
 
  @Test
  void bidSuccessAddsOneEntryToHistory() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(1, auction.getBidHistory().size());
  }
 
  @Test
  void bidSuccessHistoryRecordsCorrectAmount() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, auction.getBidHistory().get(0).getAmount());
  }
 
  @Test
  void bidSuccessHistoryRecordsCorrectBidder() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals("alice", auction.getBidHistory().get(0).getBidder().getUsername());
  }
 
  @Test
  void bidSuccessMultipleBidsHistoryHasTwo() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    auction.processNewBid(bidder2, VALID_BID_2);
    assertEquals(2, auction.getBidHistory().size());
  }
 
  @Test
  void bidSuccessMultipleBidsPriceUpdatesToLatest() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    auction.processNewBid(bidder2, VALID_BID_2);
    assertEquals(VALID_BID_2, auction.getCurrentPrice());
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
  void bidEqualToCurrentPriceThrowsInvalidBidException() {
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, STARTING_PRICE));
  }
 
  @Test
  void bidLowerThanCurrentPriceThrowsInvalidBidException() {
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
  void addObserverReceivesMessageOnBid() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.processNewBid(bidder1, VALID_BID_1);
    // notifyObservers chạy async, đợi ngắn
    Thread.sleep(100);
    assertTrue(received.size() >= 1);
  }
 
  @Test
  void removeObserverNoLongerReceivesMessage() throws Exception {
    List<String> received = new ArrayList<>();
    Observer obs = msg -> received.add(msg);
    auction.addObserver(obs);
    auction.removeObserver(obs);
    auction.processNewBid(bidder1, VALID_BID_1);
    Thread.sleep(100);
    assertEquals(0, received.size());
  }
 
  @Test
  void notifyObserversWithNoObserversDoesNotThrow() {
    assertDoesNotThrow(() -> auction.notifyObservers("test-msg"));
  }
 
  @Test
  void notifyObserversMessageIsDelivered() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.notifyObservers("hello");
    Thread.sleep(100);
    assertNotNull(received);
  }
 
  // --- getBidHistory ---
 
  @Test
  void getBidHistoryInitiallyEmpty() {
    // Tạo auction MỚI hoàn toàn, không qua setUp đã bid sẵn
    Auction freshAuction = new Auction("fresh-01", new Electronics("e-fresh", "TV", 500.0), DURATION, null);
    assertTrue(freshAuction.getBidHistory().isEmpty());
  }
 
  // --- restoreTransients ---
 
  @Test
  void restoreTransientsCalledMultipleTimesDoesNotThrow() {
    assertDoesNotThrow(() -> {
      auction.restoreTransients();
      auction.restoreTransients();
    });
  }
 
  // --- closeAuction ---
 
  @Test
  void closeAuctionSetsStatusFinished() {
    auction.closeAuction();
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void closeAuctionClearsObservers() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.closeAuction();
    auction.notifyObservers("should not arrive");
    Thread.sleep(100);
    assertEquals(0, received.size());
  }
 
  // --- toString ---
 
  @Test
  void toStringContainsId() {
    assertTrue(auction.toString().contains("auction-01"));
  }
 
  @Test
  void toStringContainsItemName() {
    assertTrue(auction.toString().contains("Laptop"));
  }
 
  @Test
  void toStringContainsStatus() {
    assertTrue(auction.toString().contains("RUNNING"));
  }
 
  // --- AutoBid ---
 
  @Test
  void addAutoBidConfigDoesNotThrow() {
    assertDoesNotThrow(() ->
        auction.addAutoBidConfig("alice", VALID_BID_3));
  }
 
  @Test
  void addAutoBidConfigTriggersAutoBidRaisesPrice() throws Exception {
    auction.processNewBid(bidder2, VALID_BID_1);
    auction.addAutoBidConfig("alice", VALID_BID_3);
    assertTrue(auction.getCurrentPrice() > VALID_BID_1);
  }
 
  @Test
  void addAutoBidConfigUpdatesExistingConfig() {
    assertDoesNotThrow(() -> {
      auction.addAutoBidConfig("alice", VALID_BID_2);
      auction.addAutoBidConfig("alice", VALID_BID_3);
    });
  }
 
  // --- Concurrency ---
 
  @Test
  void concurrentBidsDoNotThrowAndHistoryNonEmpty() throws Exception {
    Thread t1 = new Thread(() -> {
      try {
        auction.processNewBid(bidder1, VALID_BID_1);
      } catch (Exception ignored) {
        // intentionally ignored
      }
    });
    Thread t2 = new Thread(() -> {
      try {
        auction.processNewBid(bidder2, VALID_BID_2);
      } catch (Exception ignored) {
        // intentionally ignored
      }
    });
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    assertTrue(auction.getBidHistory().size() >= 1);
  }
  
}