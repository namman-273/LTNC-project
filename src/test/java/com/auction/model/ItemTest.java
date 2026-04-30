package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
 
import org.junit.jupiter.api.Test;
 
public class ItemTest {
 
  private static final double PRICE = 500.0;
  private static final double NEW_PRICE = 999.0;
 
  @Test
  void electronicsIsInstanceOfItem() {
    assertInstanceOf(Item.class, new Electronics("e1", "Phone", PRICE));
  }
 
  @Test
  void artIsInstanceOfItem() {
    assertInstanceOf(Item.class, new Art("a1", "Painting", PRICE));
  }
 
  @Test
  void vehicleIsInstanceOfItem() {
    assertInstanceOf(Item.class, new Vehicle("v1", "Car", PRICE));
  }
 
  @Test
  void getIdReturnsCorrectId() {
    Item item = new Electronics("e2", "TV", PRICE);
    assertEquals("e2", item.getId());
  }
 
  @Test
  void getItemNameReturnsCorrectName() {
    Item item = new Electronics("e3", "Laptop", PRICE);
    assertEquals("Laptop", item.getItemName());
  }
 
  @Test
  void getStartingPriceReturnsCorrectPrice() {
    Item item = new Art("a2", "Sculpture", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
 
  @Test
  void currentPriceDefaultEqualsStartingPrice() {
    Item item = new Electronics("e4", "Monitor", PRICE);
    assertEquals(PRICE, item.getCurrentPrice());
  }
 
  @Test
  void setCurrentPriceUpdatesCorrectly() {
    Item item = new Art("a3", "Vase", PRICE);
    item.setCurrentPrice(NEW_PRICE);
    assertEquals(NEW_PRICE, item.getCurrentPrice());
  }
 
  @Test
  void highestBidderDefaultIsNoBidsYet() {
    Item item = new Vehicle("v2", "BMW", PRICE);
    assertEquals("No bids yet", item.getHighestBidder());
  }
 
  @Test
  void setHighestBidderUpdatesCorrectly() {
    Item item = new Electronics("e5", "TV", PRICE);
    item.setHighestBidder("alice");
    assertEquals("alice", item.getHighestBidder());
  }
 
  @Test
  void electronicsDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Electronics("e6", "Monitor", PRICE).displayInfo());
  }
 
  @Test
  void artDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Art("a4", "Mona Lisa", PRICE).displayInfo());
  }
 
  @Test
  void vehicleDisplayInfoDoesNotThrow() {
    assertDoesNotThrow(() -> new Vehicle("v3", "Truck", PRICE).displayInfo());
  }
 
  @Test
  void setIdUpdatesCorrectly() {
    Item item = new Electronics("old", "TV", PRICE);
    item.setId("new-id");
    assertEquals("new-id", item.getId());
  }
}
