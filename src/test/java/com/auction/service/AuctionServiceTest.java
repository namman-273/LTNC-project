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
    // Reset Singleton trước mỗi test để test độc lập nhau
    Field field = AuctionService.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
    service = AuctionService.getInstance();
  }
 
  // ----------------------------------------------------------------
  // Singleton
  // ----------------------------------------------------------------
 
  @Test
  void testGetInstance_calledTwice_returnsSameObject() {
    AuctionService first = AuctionService.getInstance();
    AuctionService second = AuctionService.getInstance();
    assertSame(first, second);
  }
 
  // ----------------------------------------------------------------
  // addAuction / getAuctionById
  // ----------------------------------------------------------------
 
  @Test
  void testAddAuction_thenGetById_returnsCorrectAuction() {
    Item item = new Electronics("e1", "Phone", 500.0);
    Auction auction = new Auction("a1", item, DURATION);
    service.addAuction(auction);
    assertNotNull(service.getAuctionById("a1"));
  }
 
  @Test
  void testAddAuction_thenGetById_hasCorrectId() {
    Item item = new Electronics("e2", "TV", 800.0);
    Auction auction = new Auction("a2", item, DURATION);
    service.addAuction(auction);
    assertEquals("a2", service.getAuctionById("a2").getId());
  }
 
  @Test
  void testAddAuction_nullInput_doesNotThrow() {
    assertDoesNotThrow(() -> service.addAuction(null));
  }
 
  @Test
  void testGetAuctionById_nonExistentId_returnsNull() {
    assertNull(service.getAuctionById("ghost-id"));
  }
 
  // ----------------------------------------------------------------
  // getAllAuctions
  // ----------------------------------------------------------------
 
  @Test
  void testGetAllAuctions_afterAddingTwo_sizeAtLeastTwo() {
    service.addAuction(new Auction("a3", new Art("art1", "Painting", 200.0), DURATION));
    service.addAuction(new Auction("a4", new Vehicle("v1", "Honda", 5000.0), DURATION));
    assertTrue(service.getAllAuctions().size() >= 2);
  }
 
  // ----------------------------------------------------------------
  // endAuction — status
  // ----------------------------------------------------------------
 
  @Test
  void testEndAuction_runningStatus_setsFinished() {
    Item item = new Electronics("e3", "Tablet", 300.0);
    Auction auction = new Auction("a-end-1", item, DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    service.addAuction(auction);
    service.endAuction("a-end-1");
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void testEndAuction_openStatus_alsoSetsFinished() {
    Item item = new Electronics("e4", "Camera", 400.0);
    Auction auction = new Auction("a-end-2", item, DURATION);
    // status mặc định là OPEN
    service.addAuction(auction);
    service.endAuction("a-end-2");
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void testEndAuction_alreadyFinished_doesNotThrow() {
    Item item = new Art("art2", "Vase", 300.0);
    Auction auction = new Auction("a-end-3", item, DURATION);
    auction.setStatus(AuctionStatus.FINISHED);
    service.addAuction(auction);
    assertDoesNotThrow(() -> service.endAuction("a-end-3"));
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void testEndAuction_nonExistentId_doesNotThrow() {
    assertDoesNotThrow(() -> service.endAuction("non-existent"));
  }
 
  // ----------------------------------------------------------------
  // endAuction — xác định winner
  // ----------------------------------------------------------------
 
  @Test
  void testEndAuction_withBids_notifiesWinnerUsername() throws Exception {
    Item item = new Electronics("e5", "Laptop", 400.0);
    Auction auction = new Auction("a-end-4", item, DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    service.addAuction(auction);
 
    Bidder bidder1 = new Bidder("charlie", "pw");
    Bidder bidder2 = new Bidder("diana", "pw");
    auction.processNewBid(bidder1, PRICE_LOW);
    auction.processNewBid(bidder2, PRICE_HIGH);
 
    // Dùng observer để bắt thông báo kết quả
    final String[] lastMsg = {""};
    auction.addObserver(msg -> lastMsg[0] = msg);
 
    service.endAuction("a-end-4");
 
    assertTrue(lastMsg[0].contains("diana"));
  }
 
  @Test
  void testEndAuction_withBids_notifiesWinnerBidAmount() throws Exception {
    Item item = new Electronics("e6", "Monitor", 400.0);
    Auction auction = new Auction("a-end-5", item, DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    service.addAuction(auction);
 
    Bidder bidder1 = new Bidder("eve", "pw");
    Bidder bidder2 = new Bidder("frank", "pw");
    auction.processNewBid(bidder1, PRICE_LOW);
    auction.processNewBid(bidder2, PRICE_HIGH);
 
    final String[] lastMsg = {""};
    auction.addObserver(msg -> lastMsg[0] = msg);
 
    service.endAuction("a-end-5");
 
    assertTrue(lastMsg[0].contains(String.valueOf(PRICE_HIGH)));
  }
 
  @Test
  void testEndAuction_noBids_notifiesNoWinner() {
    Item item = new Art("art3", "Sculpture", 1000.0);
    Auction auction = new Auction("a-end-6", item, DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    service.addAuction(auction);
 
    final String[] lastMsg = {""};
    auction.addObserver(msg -> lastMsg[0] = msg);
 
    service.endAuction("a-end-6");
 
    assertTrue(lastMsg[0].contains("No winner"));
  }
 
  // ----------------------------------------------------------------
  // getItemInAuction
  // ----------------------------------------------------------------
 
  @Test
  void testGetItemInAuction_existingId_returnsCorrectItem() {
    Item item = new Vehicle("v2", "Toyota", 8000.0);
    service.addAuction(new Auction("a-item-1", item, DURATION));
    assertEquals(item, service.getItemInAuction("a-item-1"));
  }
 
  @Test
  void testGetItemInAuction_nonExistentId_returnsNull() {
    assertNull(service.getItemInAuction("ghost-id"));
  }
 
  // ----------------------------------------------------------------
  // setAuctions / getAuctionsMap
  // ----------------------------------------------------------------
 
  @Test
  void testSetAuctions_null_doesNotThrow() {
    assertDoesNotThrow(() -> service.setAuctions(null));
  }
 
  @Test
  void testGetAuctionsMap_notNull() {
    assertNotNull(service.getAuctionsMap());
  }
}