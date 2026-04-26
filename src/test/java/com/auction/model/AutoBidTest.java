package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import org.junit.jupiter.api.Test;
 
public class AutoBidTest {
 
  private static final double MAX_BID = 2000.0;
  private static final double INCREMENT = 100.0;
 
  @Test
  void autoBidGetBidderIdReturnsCorrectId() {
    AutoBid autoBid = new AutoBid("alice", MAX_BID, INCREMENT);
    assertEquals("alice", autoBid.getBidderId());
  }
 
  @Test
  void autoBidGetMaxBidReturnsCorrectMaxBid() {
    AutoBid autoBid = new AutoBid("bob", MAX_BID, INCREMENT);
    assertEquals(MAX_BID, autoBid.getMaxBid());
  }
 
  @Test
  void autoBidGetIncrementReturnsCorrectIncrement() {
    AutoBid autoBid = new AutoBid("carol", MAX_BID, INCREMENT);
    assertEquals(INCREMENT, autoBid.getIncrement());
  }
 
  @Test
  void compareToHigherMaxBidHasPriority() {
    AutoBid high = new AutoBid("alice", 2000.0, INCREMENT);
    AutoBid low = new AutoBid("bob", 1000.0, INCREMENT);
    assertTrue(high.compareTo(low) < 0);
  }
 
  @Test
  void compareToLowerMaxBidHasLowerPriority() {
    AutoBid high = new AutoBid("alice", 2000.0, INCREMENT);
    AutoBid low = new AutoBid("bob", 1000.0, INCREMENT);
    assertTrue(low.compareTo(high) > 0);
  }
 
  @Test
  void compareToSameMaxBidEarlierTimestampHasPriority() throws InterruptedException {
    AutoBid first = new AutoBid("alice", MAX_BID, INCREMENT);
    Thread.sleep(5);
    AutoBid second = new AutoBid("bob", MAX_BID, INCREMENT);
    assertTrue(first.compareTo(second) < 0);
  }
 
  @Test
  void compareToSameObjectReturnsZero() {
    AutoBid autoBid = new AutoBid("alice", MAX_BID, INCREMENT);
    assertEquals(0, autoBid.compareTo(autoBid));
  }
}