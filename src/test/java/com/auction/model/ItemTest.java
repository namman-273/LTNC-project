package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
 
import org.junit.jupiter.api.Test;
 
public class ItemTest {
 
  private static final double PRICE = 500.0;
  private static final double NEW_PRICE = 999.0;
 
  // ----------------------------------------------------------------
  // Item — currentPrice
  // ----------------------------------------------------------------
 
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
 
  // ----------------------------------------------------------------
  // Item — highestBidder
  // ----------------------------------------------------------------
 
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
 
  // ----------------------------------------------------------------
  // Item — getters
  // ----------------------------------------------------------------
 
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
 
  // ----------------------------------------------------------------
  // Item subclasses — instanceof
  // ----------------------------------------------------------------
 
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
 
  // ----------------------------------------------------------------
  // Item subclasses — displayInfo
  // ----------------------------------------------------------------
 
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
}
 