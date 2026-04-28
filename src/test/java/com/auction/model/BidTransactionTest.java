package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class BidTransactionTest {
 
  private static final double AMOUNT = 750.0;
  private static final double NEW_AMOUNT = 999.0;
 
  private Bidder bidder;
  private BidTransaction transaction;
 
  @BeforeEach
  void setUp() {
    bidder = new Bidder("alice", "pw");
    transaction = new BidTransaction(bidder, AMOUNT);
  }
 
  @Test
  void getBidderReturnsCorrectBidder() {
    assertEquals(bidder, transaction.getBidder());
  }
 
  @Test
  void getAmountReturnsCorrectAmount() {
    assertEquals(AMOUNT, transaction.getAmount());
  }
 
  @Test
  void getTimestampIsNotNull() {
    assertNotNull(transaction.getTimestamp());
  }
 
  @Test
  void getTimestampIsAfterTestStart() {
    LocalDateTime before = LocalDateTime.now().minusSeconds(1);
    BidTransaction tx = new BidTransaction(bidder, AMOUNT);
    assertTrue(tx.getTimestamp().isAfter(before));
  }
 
  @Test
  void setBidderUpdatesCorrectly() {
    Bidder newBidder = new Bidder("bob", "pw");
    transaction.setBidder(newBidder);
    assertEquals(newBidder, transaction.getBidder());
  }
 
  @Test
  void setAmountUpdatesCorrectly() {
    transaction.setAmount(NEW_AMOUNT);
    assertEquals(NEW_AMOUNT, transaction.getAmount());
  }
 
  @Test
  void setTimestampUpdatesCorrectly() {
    LocalDateTime newTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
    transaction.setTimestamp(newTime);
    assertEquals(newTime, transaction.getTimestamp());
  }
 
  @Test
  void toStringContainsBidderUsername() {
    assertTrue(transaction.toString().contains("alice"));
  }
 
  @Test
  void toStringContainsFormattedAmount() {
    assertTrue(transaction.toString().contains("750.00"));
  }
 
  @Test
  void toStringContainsTimestamp() {
    assertNotNull(transaction.toString());
    assertTrue(transaction.toString().startsWith("["));
  }
}