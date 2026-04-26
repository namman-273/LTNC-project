package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class ModelTest {
 
  private static final double PRICE = 500.0;
  private static final double NEW_PRICE = 999.0;
  private static final double BID_AMOUNT = 750.0;
  private static final double NEW_AMOUNT = 999.0;
 
  private Bidder bidder;
  private BidTransaction transaction;
 
  @BeforeEach
  void setUp() {
    bidder = new Bidder("alice", "pw");
    transaction = new BidTransaction(bidder, BID_AMOUNT);
  }
 
  // ================================================================
  // Item
  // ================================================================
 
  @Test
  void itemCurrentPriceDefaultEqualsStartingPrice() {
    Item item = new Electronics("e1", "Phone", PRICE);
    assertEquals(PRICE, item.getCurrentPrice());
  }
 
  @Test
  void itemSetCurrentPriceUpdatesSuccessfully() {
    Item item = new Art("a1", "Painting", PRICE);
    item.setCurrentPrice(NEW_PRICE);
    assertEquals(NEW_PRICE, item.getCurrentPrice());
  }
 
  @Test
  void itemHighestBidderDefaultIsNoBidsYet() {
    Item item = new Vehicle("v1", "BMW", PRICE);
    assertEquals("No bids yet", item.getHighestBidder());
  }
 
  @Test
  void itemSetHighestBidderUpdatesSuccessfully() {
    Item item = new Electronics("e2", "TV", PRICE);
    item.setHighestBidder("alice");
    assertEquals("alice", item.getHighestBidder());
  }
 
  @Test
  void itemGetItemNameReturnsCorrectName() {
    Item item = new Electronics("e3", "Laptop", PRICE);
    assertEquals("Laptop", item.getItemName());
  }
 
  @Test
  void itemGetStartingPriceReturnsCorrectPrice() {
    Item item = new Art("a2", "Sculpture", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
 
  @Test
  void itemGetIdReturnsCorrectId() {
    Item item = new Vehicle("v2", "Honda", PRICE);
    assertEquals("v2", item.getId());
  }
 
  @Test
  void electronicsIsInstanceOfItem() {
    assertInstanceOf(Item.class, new Electronics("e4", "Monitor", PRICE));
  }
 
  @Test
  void artIsInstanceOfItem() {
    assertInstanceOf(Item.class, new Art("a3", "Vase", PRICE));
  }
 
  @Test
  void vehicleIsInstanceOfItem() {
    assertInstanceOf(Item.class, new Vehicle("v3", "Truck", PRICE));
  }
 
  @Test
  void electronicsDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Electronics("e5", "Monitor", PRICE).displayInfo());
  }
 
  @Test
  void artDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Art("a4", "Sculpture", PRICE).displayInfo());
  }
 
  @Test
  void vehicleDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Vehicle("v4", "Truck", PRICE).displayInfo());
  }
 
  // ================================================================
  // User (Bidder / Seller / Admin)
  // ================================================================
 
  @Test
  void bidderGetRoleReturnsBidder() {
    assertEquals("BIDDER", new Bidder("bob", "pw").getRole());
  }
 
  @Test
  void sellerGetRoleReturnsSeller() {
    assertEquals("SELLER", new Seller("carol", "pw").getRole());
  }
 
  @Test
  void adminGetRoleReturnsAdmin() {
    assertEquals("ADMIN", new Admin("dave", "pw").getRole());
  }
 
  @Test
  void getUsernameReturnsCorrectUsername() {
    assertEquals("eve", new Bidder("eve", "pw").getUsername());
  }
 
  @Test
  void getIdEqualsUsername() {
    assertEquals("frank", new Bidder("frank", "pw").getId());
  }
 
  @Test
  void bidderIsInstanceOfUser() {
    assertInstanceOf(User.class, new Bidder("grace", "pw"));
  }
 
  @Test
  void sellerIsInstanceOfUser() {
    assertInstanceOf(User.class, new Seller("henry", "pw"));
  }
 
  @Test
  void adminIsInstanceOfUser() {
    assertInstanceOf(User.class, new Admin("ivan", "pw"));
  }
 
  @Test
  void checkPasswordCorrectReturnsTrue() {
    assertTrue(new Bidder("jack", "mypassword").checkPassword("mypassword"));
  }
 
  @Test
  void checkPasswordWrongReturnsFalse() {
    assertFalse(new Bidder("kate", "correct").checkPassword("wrong"));
  }
 
  @Test
  void checkPasswordEmptyStringDoesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("leo", "pw").checkPassword(""));
  }
 
  @Test
  void bidderDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("mia", "pw").displayInfo());
  }
 
  @Test
  void sellerDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Seller("noah", "pw").displayInfo());
  }
 
  @Test
  void adminDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Admin("oscar", "pw").displayInfo());
  }
 
  @Test
  void updateDoesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("paul", "pw").update("NOTIFY_MSG"));
  }
 
  @Test
  void toStringContainsUsername() {
    assertTrue(new Bidder("quinn", "pw").toString().contains("quinn"));
  }
 
  @Test
  void toStringContainsRole() {
    assertTrue(new Bidder("rose", "pw").toString().contains("BIDDER"));
  }
 
  // ================================================================
  // BidTransaction
  // ================================================================
 
  @Test
  void bidTransactionGetBidderReturnsCorrectBidder() {
    assertEquals(bidder, transaction.getBidder());
  }
 
  @Test
  void bidTransactionGetAmountReturnsCorrectAmount() {
    assertEquals(BID_AMOUNT, transaction.getAmount());
  }
 
  @Test
  void bidTransactionGetTimestampIsNotNull() {
    assertNotNull(transaction.getTimestamp());
  }
 
  @Test
  void bidTransactionGetTimestampIsRecentTime() {
    LocalDateTime before = LocalDateTime.now().minusSeconds(1);
    BidTransaction tx = new BidTransaction(bidder, BID_AMOUNT);
    assertTrue(tx.getTimestamp().isAfter(before));
  }
 
  @Test
  void bidTransactionSetBidderUpdatesCorrectly() {
    Bidder newBidder = new Bidder("sam", "pw");
    transaction.setBidder(newBidder);
    assertEquals(newBidder, transaction.getBidder());
  }
 
  @Test
  void bidTransactionSetAmountUpdatesCorrectly() {
    transaction.setAmount(NEW_AMOUNT);
    assertEquals(NEW_AMOUNT, transaction.getAmount());
  }
 
  @Test
  void bidTransactionSetTimestampUpdatesCorrectly() {
    LocalDateTime newTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
    transaction.setTimestamp(newTime);
    assertEquals(newTime, transaction.getTimestamp());
  }
 
  @Test
  void bidTransactionToStringContainsBidderUsername() {
    assertTrue(transaction.toString().contains("alice"));
  }
 
  @Test
  void bidTransactionToStringContainsAmount() {
    assertTrue(transaction.toString().contains("750.00"));
  }
}