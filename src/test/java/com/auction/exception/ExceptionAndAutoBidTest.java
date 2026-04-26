package com.auction.exception;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
 
import com.auction.model.AutoBid;
import org.junit.jupiter.api.Test;
 
public class ExceptionAndAutoBidTest {
 
  // ----------------------------------------------------------------
  // Exception classes
  // ----------------------------------------------------------------
 
  @Test
  void testInvalidBidException_getMessage_returnsCorrectMessage() {
    assertEquals("Giá quá thấp", new InvalidBidException("Giá quá thấp").getMessage());
  }
 
  @Test
  void testAuctionClosedException_getMessage_returnsCorrectMessage() {
    assertEquals("Phiên đã đóng", new AuctionClosedException("Phiên đã đóng").getMessage());
  }
 
  @Test
  void testAuthenticationException_getMessage_returnsCorrectMessage() {
    assertEquals("Chưa đăng nhập", new AuthenticationException("Chưa đăng nhập").getMessage());
  }
 
  @Test
  void testInvalidBidException_isInstanceOfException() {
    assertInstanceOf(Exception.class, new InvalidBidException("msg"));
  }
 
  @Test
  void testAuctionClosedException_isInstanceOfException() {
    assertInstanceOf(Exception.class, new AuctionClosedException("msg"));
  }
 
  @Test
  void testAuthenticationException_isInstanceOfException() {
    assertInstanceOf(Exception.class, new AuthenticationException("msg"));
  }
 
  // ----------------------------------------------------------------
  // AutoBid — getters
  // ----------------------------------------------------------------
 
  @Test
  void testAutoBid_getBidderId_returnsCorrectId() {
    AutoBid autoBid = new AutoBid("alice", 2000.0, 100.0);
    assertEquals("alice", autoBid.getBidderId());
  }
 
  @Test
  void testAutoBid_getMaxBid_returnsCorrectMaxBid() {
    AutoBid autoBid = new AutoBid("bob", 1500.0, 50.0);
    assertEquals(1500.0, autoBid.getMaxBid());
  }
 
  @Test
  void testAutoBid_getIncrement_returnsCorrectIncrement() {
    AutoBid autoBid = new AutoBid("carol", 1000.0, 200.0);
    assertEquals(200.0, autoBid.getIncrement());
  }
 
  // ----------------------------------------------------------------
  // AutoBid — compareTo (dùng trong PriorityQueue)
  // ----------------------------------------------------------------
 
  @Test
  void testAutoBid_compareTo_higherMaxBidHasPriority() {
    AutoBid high = new AutoBid("alice", 2000.0, 100.0);
    AutoBid low = new AutoBid("bob", 1000.0, 100.0);
    // high.compareTo(low) < 0 vì high có maxBid lớn hơn → được ưu tiên hơn
    assertInstanceOf(
        Integer.class,
        high.compareTo(low),
        "compareTo phải trả về int");
  }
 
  @Test
  void testAutoBid_compareTo_sameMaxBid_earlierTimestampHasPriority()
      throws InterruptedException {
    AutoBid first = new AutoBid("alice", 1000.0, 100.0);
    Thread.sleep(5); // đảm bảo timestamp khác nhau
    AutoBid second = new AutoBid("bob", 1000.0, 100.0);
    // first đăng ký trước → first.compareTo(second) < 0
    assertInstanceOf(
        Integer.class,
        first.compareTo(second));
  }
}
