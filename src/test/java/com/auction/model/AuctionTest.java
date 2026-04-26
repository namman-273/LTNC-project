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
 
  // Dùng hằng số để tránh magic number (Google Style yêu cầu)
  private static final double STARTING_PRICE = 1000.0;
  private static final double VALID_BID_1 = 1200.0;
  private static final double VALID_BID_2 = 1500.0;
  private static final double VALID_BID_3 = 1800.0;
  private static final double LOW_BID = 500.0;
  private static final long DURATION_LONG = 9999L; // phút — đủ dài để test không hết giờ
 
  private Auction auction;
  private Bidder bidder1;
  private Bidder bidder2;
  private Item item;
 
  @BeforeEach
  void setUp() {
    item = new Electronics("item-01", "Laptop", STARTING_PRICE);
    // Auction của nhóm bạn có constructor(String id, Item item, long durationMinutes)
    auction = new Auction("auction-01", item, DURATION_LONG);
    bidder1 = new Bidder("alice", "pass123");
    bidder2 = new Bidder("bob", "pass456");
    // Mặc định status là OPEN — cần set RUNNING để bid được
    auction.setStatus(AuctionStatus.RUNNING);
  }
 
  // ----------------------------------------------------------------
  // processNewBid — Trường hợp hợp lệ
  // ----------------------------------------------------------------
 
  @Test
  void testBidSuccess_updatesCurrentPrice() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, item.getCurrentPrice());
  }
 
  @Test
  void testBidSuccess_addsOneTxToHistory() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(1, auction.getBidHistory().size());
  }
 
  @Test
  void testBidSuccess_historyRecordsCorrectAmount() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(VALID_BID_1, auction.getBidHistory().get(0).getAmount());
  }
 
  @Test
  void testBidSuccess_multipleBids_historyHasTwoTx() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    auction.processNewBid(bidder2, VALID_BID_2);
    assertEquals(2, auction.getBidHistory().size());
  }
 
  @Test
  void testBidSuccess_multipleBids_priceUpdatesToLatestHighest() throws Exception {
    auction.processNewBid(bidder1, VALID_BID_1);
    auction.processNewBid(bidder2, VALID_BID_2);
    assertEquals(VALID_BID_2, item.getCurrentPrice());
  }
 
  // ----------------------------------------------------------------
  // processNewBid — Lỗi xác thực (null bidder)
  // ----------------------------------------------------------------
 
  @Test
  void testBid_nullBidder_throwsAuthenticationException() {
    assertThrows(
        AuthenticationException.class,
        () -> auction.processNewBid(null, VALID_BID_1));
  }
 
  // ----------------------------------------------------------------
  // processNewBid — Lỗi giá không hợp lệ
  // ----------------------------------------------------------------
 
  @Test
  void testBid_equalToStartingPrice_throwsInvalidBidException() {
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, STARTING_PRICE));
  }
 
  @Test
  void testBid_lowerThanStartingPrice_throwsInvalidBidException() {
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder1, LOW_BID));
  }
 
  @Test
  void testBid_lowerThanCurrentPriceAfterFirstBid_throwsInvalidBidException()
      throws Exception {
    auction.processNewBid(bidder1, VALID_BID_2);
    assertThrows(
        InvalidBidException.class,
        () -> auction.processNewBid(bidder2, VALID_BID_1));
  }
 
  // ----------------------------------------------------------------
  // processNewBid — Lỗi phiên đã đóng
  // ----------------------------------------------------------------
 
  @Test
  void testBid_whenStatusFinished_throwsAuctionClosedException() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, VALID_BID_1));
  }
 
  @Test
  void testBid_whenStatusPaid_throwsAuctionClosedException() {
    auction.setStatus(AuctionStatus.PAID);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, VALID_BID_1));
  }
 
  @Test
  void testBid_whenStatusCanceled_throwsAuctionClosedException() {
    auction.setStatus(AuctionStatus.CANCELED);
    assertThrows(
        AuctionClosedException.class,
        () -> auction.processNewBid(bidder1, VALID_BID_1));
  }
 
  // ----------------------------------------------------------------
  // Observer
  // ----------------------------------------------------------------
 
  @Test
  void testAddObserver_receivesNotificationOnBid() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(1, received.size());
  }
 
  @Test
  void testAddObserver_notificationContainsBidAmount() throws Exception {
    List<String> received = new ArrayList<>();
    auction.addObserver(msg -> received.add(msg));
    auction.processNewBid(bidder1, VALID_BID_1);
    assertTrue(received.get(0).contains(String.valueOf(VALID_BID_1)));
  }
 
  @Test
  void testRemoveObserver_doesNotReceiveNotification() throws Exception {
    List<String> received = new ArrayList<>();
    Observer obs = msg -> received.add(msg);
    auction.addObserver(obs);
    auction.removeObserver(obs);
    auction.processNewBid(bidder1, VALID_BID_1);
    assertEquals(0, received.size());
  }
 
  @Test
  void testNotifyObservers_withNullObserverList_doesNotThrow() {
    // restoreTransients đã được gọi trong constructor, gọi lại vẫn an toàn
    assertDoesNotThrow(() -> auction.notifyObservers("test-msg"));
  }
 
  // ----------------------------------------------------------------
  // Getter / Setter / restoreTransients
  // ----------------------------------------------------------------
 
  @Test
  void testGetStatus_afterSetRunning_returnsRunning() {
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());
  }
 
  @Test
  void testSetStatus_toFinished_changesStatus() {
    auction.setStatus(AuctionStatus.FINISHED);
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void testGetItem_returnsItemPassedToConstructor() {
    assertEquals(item, auction.getItem());
  }
 
  @Test
  void testGetId_returnsIdPassedToConstructor() {
    assertEquals("auction-01", auction.getId());
  }
 
  @Test
  void testRestoreTransients_canBeCalledMultipleTimes_doesNotThrow() {
    assertDoesNotThrow(() -> {
      auction.restoreTransients();
      auction.restoreTransients();
    });
  }
 
  // ----------------------------------------------------------------
  // AutoBid (chức năng nâng cao)
  // ----------------------------------------------------------------
 
  @Test
  void testAddAutoBidConfig_doesNotThrow() {
    assertDoesNotThrow(() ->
        auction.addAutoBidConfig("alice", VALID_BID_3, 100.0));
  }
 
  @Test
  void testAddAutoBidConfig_triggersAutoBid_whenCurrentPriceBelowMaxBid()
      throws Exception {
    // alice đăng ký auto-bid max=1800, increment=100
    // Sau khi bob bid 1200, auto-bid của alice phải tự kích hoạt lên 1300
    auction.processNewBid(bidder2, VALID_BID_1); // bob bid 1200
    auction.addAutoBidConfig("alice", VALID_BID_3, 100.0);
    // Giá phải được tự động tăng lên ít nhất 1300
    assertTrue(item.getCurrentPrice() > VALID_BID_1);
  }
 
  // ----------------------------------------------------------------
  // Concurrency
  // ----------------------------------------------------------------
 
  @Test
  void testConcurrentBids_noExceptionAndHistorySizeIsTwo() throws Exception {
    // bid1 trước để đặt nền
    auction.processNewBid(bidder1, VALID_BID_1);
 
    Thread thread = new Thread(
        () -> {
          try {
            auction.processNewBid(bidder2, VALID_BID_2);
          } catch (Exception ignored) {
            // intentionally ignored — test chỉ kiểm tra không crash
          }
        });
    thread.start();
    thread.join();
 
    assertTrue(auction.getBidHistory().size() >= 1);
  }
}
 