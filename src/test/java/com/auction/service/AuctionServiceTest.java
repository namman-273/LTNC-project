package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.model.Art;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionServiceTest {
 
  private static final long DURATION = 9999L;
  private static final double PRICE_LOW = 450.0;
  private static final double PRICE_HIGH = 600.0;
 
  private AuctionService service;
 
  @BeforeEach
  void setUp() throws Exception {
    Field field = AuctionService.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
    service = AuctionService.getInstance();
  }
 
  // --- Singleton ---
 
  @Test
  void getInstanceReturnsSameObject() {
    assertSame(AuctionService.getInstance(), AuctionService.getInstance());
  }
 
  // --- addAuction / getAuctionById ---
 
  @Test
  void addAuctionThenGetByIdReturnsAuction() {
    Auction a = new Auction("a1", new Electronics("e1", "Phone", 500.0), DURATION);
    service.addAuction(a);
    assertNotNull(service.getAuctionById("a1"));
  }
 
  @Test
  void addAuctionThenGetByIdHasCorrectId() {
    Auction a = new Auction("a2", new Electronics("e2", "TV", 800.0), DURATION);
    service.addAuction(a);
    assertEquals("a2", service.getAuctionById("a2").getId());
  }
 
  @Test
  void addAuctionNullDoesNotThrow() {
    assertDoesNotThrow(() -> service.addAuction(null));
  }
 
  @Test
  void getAuctionByIdNonExistentReturnsNull() {
    assertNull(service.getAuctionById("ghost"));
  }
 
  // --- getAllAuctions ---
 
  @Test
  void getAllAuctionsAfterAddingTwoHasSizeAtLeastTwo() {
    service.addAuction(new Auction("a3", new Art("art1", "Painting", 200.0), DURATION));
    service.addAuction(new Auction("a4", new Vehicle("v1", "Honda", 5000.0), DURATION));
    assertTrue(service.getAllAuctions().size() >= 2);
  }
 
  // --- getAuctionsMap / setAuctions ---
 
  @Test
  void getAuctionsMapIsNotNull() {
    assertNotNull(service.getAuctionsMap());
  }
 
  @Test
  void setAuctionsNullDoesNotThrow() {
    assertDoesNotThrow(() -> service.setAuctions(null));
  }
 
  // --- endAuction: status ---
 
  @Test
  void endAuctionRunningStatusSetsFinished() {
    Auction a = new Auction("end1", new Electronics("e3", "Tablet", 300.0), DURATION);
    a.setStatus(AuctionStatus.RUNNING);
    service.addAuction(a);
    service.endAuction("end1");
    assertEquals(AuctionStatus.FINISHED, a.getStatus());
  }
 
  @Test
  void endAuctionOpenStatusSetsFinished() {
    Auction a = new Auction("end2", new Electronics("e4", "Camera", 400.0), DURATION);
    service.addAuction(a);
    service.endAuction("end2");
    assertEquals(AuctionStatus.FINISHED, a.getStatus());
  }
 
  @Test
  void endAuctionAlreadyFinishedDoesNotThrow() {
    Auction a = new Auction("end3", new Art("art2", "Vase", 300.0), DURATION);
    a.setStatus(AuctionStatus.FINISHED);
    service.addAuction(a);
    assertDoesNotThrow(() -> service.endAuction("end3"));
  }
 
  @Test
  void endAuctionNonExistentIdDoesNotThrow() {
    assertDoesNotThrow(() -> service.endAuction("non-existent"));
  }
 
  // --- endAuction: xác định winner ---
 
  @Test
  void endAuctionWithBidsNotifiesWinnerUsername() throws Exception {
    Auction a = new Auction("end4", new Electronics("e5", "Laptop", 400.0), DURATION);
    a.setStatus(AuctionStatus.RUNNING);
    service.addAuction(a);
    a.processNewBid(new Bidder("charlie", "pw"), PRICE_LOW);
    a.processNewBid(new Bidder("diana", "pw"), PRICE_HIGH);
 
    final String[] lastMsg = {""};
    a.addObserver(msg -> lastMsg[0] = msg);
 
    service.endAuction("end4");
    Thread.sleep(100);
    assertTrue(lastMsg[0].contains("diana"));
  }
 
  @Test
  void endAuctionWithBidsNotifiesWinnerBidAmount() throws Exception {
    Auction a = new Auction("end5", new Electronics("e6", "Monitor", 400.0), DURATION);
    a.setStatus(AuctionStatus.RUNNING);
    service.addAuction(a);
    a.processNewBid(new Bidder("eve", "pw"), PRICE_LOW);
    a.processNewBid(new Bidder("frank", "pw"), PRICE_HIGH);
 
    final String[] lastMsg = {""};
    a.addObserver(msg -> lastMsg[0] = msg);
 
    service.endAuction("end5");
    Thread.sleep(100);
    assertTrue(lastMsg[0].contains(String.valueOf(PRICE_HIGH)));
  }
 
  @Test
  void endAuctionNoBidsNotifiesNoWinner() throws Exception {
    Auction a = new Auction("end6", new Art("art3", "Sculpture", 1000.0), DURATION);
    a.setStatus(AuctionStatus.RUNNING);
    service.addAuction(a);
 
    final String[] lastMsg = {""};
    a.addObserver(msg -> lastMsg[0] = msg);
 
    service.endAuction("end6");
    Thread.sleep(100);
    assertTrue(lastMsg[0].contains("No winner"));
  }
 
  // --- getItemInAuction ---
 
  @Test
  void getItemInAuctionExistingIdReturnsCorrectItem() {
    Item item = new Vehicle("v2", "Toyota", 8000.0);
    service.addAuction(new Auction("item1", item, DURATION));
    assertEquals(item, service.getItemInAuction("item1"));
  }
 
  @Test
  void getItemInAuctionNonExistentIdReturnsNull() {
    assertNull(service.getItemInAuction("ghost"));
  }
 
  // --- removeObserverFromAll ---
 
  @Test
  void removeObserverFromAllDoesNotThrow() {
    service.addAuction(new Auction("obs1", new Electronics("e7", "TV", 100.0), DURATION));
    assertDoesNotThrow(() -> service.removeObserverFromAll(msg -> {}));
  }
 
  // --- shutdown ---
 
  @Test
  void shutdownDoesNotThrow() {
    assertDoesNotThrow(() -> service.shutdown());
  }
}