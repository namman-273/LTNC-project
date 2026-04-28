package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import org.junit.jupiter.api.Test;
 
public class AutoBidTest {
 
  private static final double MAX_BID_HIGH = 2000.0;
  private static final double MAX_BID_LOW = 1000.0;
  private static final double INCREMENT = 100.0;
 
  @Test
  void getBidderIdReturnsCorrectId() {
    AutoBid autoBid = new AutoBid("alice", MAX_BID_HIGH, INCREMENT);
    assertEquals("alice", autoBid.getBidderId());
  }
 
  @Test
  void getMaxBidReturnsCorrectValue() {
    AutoBid autoBid = new AutoBid("bob", MAX_BID_HIGH, INCREMENT);
    assertEquals(MAX_BID_HIGH, autoBid.getMaxBid());
  }
 
  @Test
  void getIncrementReturnsCorrectValue() {
    AutoBid autoBid = new AutoBid("carol", MAX_BID_HIGH, INCREMENT);
    assertEquals(INCREMENT, autoBid.getIncrement());
  }
 
  @Test
  void compareToHigherMaxBidHasPriority() {
    AutoBid high = new AutoBid("alice", MAX_BID_HIGH, INCREMENT);
    AutoBid low = new AutoBid("bob", MAX_BID_LOW, INCREMENT);
    assertTrue(high.compareTo(low) < 0);
  }
 
  @Test
  void compareToLowerMaxBidHasLowerPriority() {
    AutoBid high = new AutoBid("alice", MAX_BID_HIGH, INCREMENT);
    AutoBid low = new AutoBid("bob", MAX_BID_LOW, INCREMENT);
    assertTrue(low.compareTo(high) > 0);
  }
 
  @Test
  void compareToSameMaxBidEarlierTimestampHasPriority() throws InterruptedException {
    AutoBid first = new AutoBid("alice", MAX_BID_HIGH, INCREMENT);
    Thread.sleep(5);
    AutoBid second = new AutoBid("bob", MAX_BID_HIGH, INCREMENT);
    assertTrue(first.compareTo(second) < 0);
  }
 
  @Test
  void compareToSameMaxBidLaterTimestampHasLowerPriority() throws InterruptedException {
    AutoBid first = new AutoBid("alice", MAX_BID_HIGH, INCREMENT);
    Thread.sleep(5);
    AutoBid second = new AutoBid("bob", MAX_BID_HIGH, INCREMENT);
    assertTrue(second.compareTo(first) > 0);
  }
 
  @Test
  void compareToSameObjectReturnsZero() {
    AutoBid autoBid = new AutoBid("alice", MAX_BID_HIGH, INCREMENT);
    assertEquals(0, autoBid.compareTo(autoBid));
  }
}