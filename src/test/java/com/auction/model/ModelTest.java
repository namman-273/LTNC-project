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
 
  // ================================================================
  // Item
  // ================================================================
 
  @Test
  void testItem_currentPrice_defaultEqualsStartingPrice() {
    Item item = new Electronics("e1", "Phone", PRICE);
    assertEquals(PRICE, item.getCurrentPrice());
  }
 
  @Test
  void testItem_setCurrentPrice_updatesSuccessfully() {
    Item item = new Art("a1", "Painting", PRICE);
    item.setCurrentPrice(NEW_PRICE);
    assertEquals(NEW_PRICE, item.getCurrentPrice());
  }
 
  @Test
  void testItem_highestBidder_defaultIsNoBidsYet() {
    Item item = new Vehicle("v1", "BMW", PRICE);
    assertEquals("No bids yet", item.getHighestBidder());
  }
 
  @Test
  void testItem_setHighestBidder_updatesSuccessfully() {
    Item item = new Electronics("e2", "TV", PRICE);
    item.setHighestBidder("alice");
    assertEquals("alice", item.getHighestBidder());
  }
 
  @Test
  void testItem_getItemName_returnsCorrectName() {
    Item item = new Electronics("e3", "Laptop", PRICE);
    assertEquals("Laptop", item.getItemName());
  }
 
  @Test
  void testItem_getStartingPrice_returnsCorrectPrice() {
    Item item = new Art("a2", "Sculpture", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
 
  @Test
  void testItem_getId_returnsCorrectId() {
    Item item = new Vehicle("v2", "Honda", PRICE);
    assertEquals("v2", item.getId());
  }
 
  @Test
  void testElectronics_isInstanceOfItem() {
    assertInstanceOf(Item.class, new Electronics("e4", "Monitor", PRICE));
  }
 
  @Test
  void testArt_isInstanceOfItem() {
    assertInstanceOf(Item.class, new Art("a3", "Vase", PRICE));
  }
 
  @Test
  void testVehicle_isInstanceOfItem() {
    assertInstanceOf(Item.class, new Vehicle("v3", "Truck", PRICE));
  }
 
  @Test
  void testElectronics_displayInfo_doesNotThrow() {
    assertDoesNotThrow(() -> new Electronics("e5", "Monitor", PRICE).displayInfo());
  }
 
  @Test
  void testArt_displayInfo_doesNotThrow() {
    assertDoesNotThrow(() -> new Art("a4", "Sculpture", PRICE).displayInfo());
  }
 
  @Test
  void testVehicle_displayInfo_doesNotThrow() {
    assertDoesNotThrow(() -> new Vehicle("v4", "Truck", PRICE).displayInfo());
  }
 
  // ================================================================
  // User (Bidder / Seller / Admin)
  // ================================================================
 
  @Test
  void testBidder_getRole_returnsBidder() {
    assertEquals("BIDDER", new Bidder("alice", "pw").getRole());
  }
 
  @Test
  void testSeller_getRole_returnsSeller() {
    assertEquals("SELLER", new Seller("bob", "pw").getRole());
  }
 
  @Test
  void testAdmin_getRole_returnsAdmin() {
    assertEquals("ADMIN", new Admin("carol", "pw").getRole());
  }
 
  @Test
  void testGetUsername_returnsCorrectUsername() {
    assertEquals("dave", new Bidder("dave", "pw").getUsername());
  }
 
  @Test
  void testGetId_equalsUsername() {
    assertEquals("eve", new Bidder("eve", "pw").getId());
  }
 
  @Test
  void testBidder_isInstanceOfUser() {
    assertInstanceOf(User.class, new Bidder("frank", "pw"));
  }
 
  @Test
  void testSeller_isInstanceOfUser() {
    assertInstanceOf(User.class, new Seller("grace", "pw"));
  }
 
  @Test
  void testAdmin_isInstanceOfUser() {
    assertInstanceOf(User.class, new Admin("henry", "pw"));
  }
 
  @Test
  void testCheckPassword_correctPassword_returnsTrue() {
    assertTrue(new Bidder("ivan", "mypassword").checkPassword("mypassword"));
  }
 
  @Test
  void testCheckPassword_wrongPassword_returnsFalse() {
    assertFalse(new Bidder("jack", "correct").checkPassword("wrong"));
  }
 
  @Test
  void testCheckPassword_emptyString_doesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("kate", "pw").checkPassword(""));
  }
 
  @Test
  void testBidder_displayInfo_doesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("leo", "pw").displayInfo());
  }
 
  @Test
  void testSeller_displayInfo_doesNotThrow() {
    assertDoesNotThrow(() -> new Seller("mia", "pw").displayInfo());
  }
 
  @Test
  void testAdmin_displayInfo_doesNotThrow() {
    assertDoesNotThrow(() -> new Admin("noah", "pw").displayInfo());
  }
 
  @Test
  void testUpdate_doesNotThrow() {
    assertDoesNotThrow(() -> new Bidder("oscar", "pw").update("NOTIFY_MSG"));
  }
 
  @Test
  void testToString_containsUsername() {
    assertTrue(new Bidder("paul", "pw").toString().contains("paul"));
  }
 
  @Test
  void testToString_containsRole() {
    assertTrue(new Bidder("quinn", "pw").toString().contains("BIDDER"));
  }
 
  // ================================================================
  // BidTransaction
  // ================================================================
 
  private Bidder bidder;
  private BidTransaction transaction;
 
  @BeforeEach
  void setUp() {
    bidder = new Bidder("alice", "pw");
    transaction = new BidTransaction(bidder, BID_AMOUNT);
  }
 
  @Test
  void testBidTransaction_getBidder_returnsCorrectBidder() {
    assertEquals(bidder, transaction.getBidder());
  }
 
  @Test
  void testBidTransaction_getAmount_returnsCorrectAmount() {
    assertEquals(BID_AMOUNT, transaction.getAmount());
  }
 
  @Test
  void testBidTransaction_getTimestamp_isNotNull() {
    assertNotNull(transaction.getTimestamp());
  }
 
  @Test
  void testBidTransaction_getTimestamp_isRecentTime() {
    LocalDateTime before = LocalDateTime.now().minusSeconds(1);
    BidTransaction tx = new BidTransaction(bidder, BID_AMOUNT);
    assertTrue(tx.getTimestamp().isAfter(before));
  }
 
  @Test
  void testBidTransaction_setBidder_updatesCorrectly() {
    Bidder newBidder = new Bidder("bob", "pw");
    transaction.setBidder(newBidder);
    assertEquals(newBidder, transaction.getBidder());
  }
 
  @Test
  void testBidTransaction_setAmount_updatesCorrectly() {
    transaction.setAmount(NEW_AMOUNT);
    assertEquals(NEW_AMOUNT, transaction.getAmount());
  }
 
  @Test
  void testBidTransaction_setTimestamp_updatesCorrectly() {
    LocalDateTime newTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
    transaction.setTimestamp(newTime);
    assertEquals(newTime, transaction.getTimestamp());
  }
 
  @Test
  void testBidTransaction_toString_containsBidderUsername() {
    assertTrue(transaction.toString().contains("alice"));
  }
 
  @Test
  void testBidTransaction_toString_containsAmount() {
    assertTrue(transaction.toString().contains("750.00"));
  }
}