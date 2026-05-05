package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Electronics;
 
public class AuctionServiceExtendedTest {

  private static final long DURATION = 9999L;
  private static final double PRICE = 500.0;

  private AuctionService service;

  @BeforeEach
  void setUp() {
    try {
      // Reset Singleton để mỗi bài Test là một môi trường sạch
      Field field = AuctionService.class.getDeclaredField("instance");
      field.setAccessible(true);
      field.set(null, null);
      service = AuctionService.getInstance();
    } catch (Exception e) {
      // Nếu lỗi do không tìm thấy field "instance", hãy check lại AuctionService.java
      service = AuctionService.getInstance();
    }
  }

  // --- createNewAuction ---
 
    @Test
    void createNewAuctionElectronicsAddsToAuctions() {
        service.createNewAuction("ELECTRONICS", "Laptop", 1000.0, DURATION, null);
        assertTrue(service.getAllAuctions().size() >= 1);
    }
 
    @Test
    void createNewAuctionArtAddsToAuctions() {
        service.createNewAuction("ART", "Painting", 500.0, DURATION, null);
        assertTrue(service.getAllAuctions().size() >= 1);
    }
 
    @Test
    void createNewAuctionVehicleAddsToAuctions() {
        service.createNewAuction("VEHICLE", "Car", 5000.0, DURATION, null);
        assertTrue(service.getAllAuctions().size() >= 1);
    }
 
    @Test
    void createNewAuctionItemHasCorrectName() {
        service.createNewAuction("ELECTRONICS", "SpecialPhone", 999.0, DURATION, null);
        boolean found = service.getAllAuctions().stream()
                .anyMatch(a -> "SpecialPhone".equals(a.getItem().getItemName()));
        assertTrue(found);
    }
 
    @Test
    void createNewAuctionItemHasCorrectStartingPrice() {
        service.createNewAuction("ART", "Vase", 777.0, DURATION, null);
        boolean found = service.getAllAuctions().stream()
                .anyMatch(a -> a.getItem().getStartingPrice() == 777.0);
        assertTrue(found);
    }
 
    @Test
    void createNewAuctionStatusIsOpen() {
        service.createNewAuction("VEHICLE", "Honda", 3000.0, DURATION, null);
        boolean found = service.getAllAuctions().stream()
                .anyMatch(a -> a.getStatus() == AuctionStatus.OPEN);
        assertTrue(found);
    }
 
    @Test
    void createMultipleAuctionsAllAdded() {
        service.createNewAuction("ELECTRONICS", "Phone1", 100.0, DURATION, null);
        service.createNewAuction("ART", "Art1", 200.0, DURATION, null);
        service.createNewAuction("VEHICLE", "Car1", 300.0, DURATION, null);
        assertTrue(service.getAllAuctions().size() >= 3);
    }
 
    // --- setAuctions ---
 
    @Test
    void setAuctionsReplacesExistingAuctions() {
        service.addAuction(new Auction("old", new Electronics("e0", "TV", PRICE), DURATION, null));
        Map<String, Auction> newMap = new HashMap<>();
        Auction newAuction = new Auction("new1", new Electronics("e1", "Phone", PRICE), DURATION, null);
        newMap.put("new1", newAuction);
        service.setAuctions(newMap);
        assertNotNull(service.getAuctionById("new1"));
    }
 
    @Test
    void setAuctionsWithEmptyMapClearsAuctions() {
        service.addAuction(new Auction("clear1", new Electronics("e2", "Watch", PRICE), DURATION, null));
        service.setAuctions(new HashMap<>());
        assertEquals(0, service.getAllAuctions().size());
    }
 
    @Test
    void setAuctionsNullKeepsExistingAuctions() {
        service.addAuction(new Auction("keep1", new Electronics("e3", "TV", PRICE), DURATION, null));
        int sizeBefore = service.getAllAuctions().size();
        service.setAuctions(null);
        assertEquals(sizeBefore, service.getAllAuctions().size());
    }
 
    // --- endAuction: winner determination ---
 
    

 
    @Test
    void endAuctionPaidStatusDoesNotSetFinished() {
        Auction a = new Auction("paid1", new Electronics("e6", "Tablet", PRICE), DURATION, null);
        a.setStatus(AuctionStatus.PAID);
        service.addAuction(a);
        service.endAuction("paid1");
        assertEquals(AuctionStatus.PAID, a.getStatus());
    }
 
    // --- removeObserverFromAll ---
 
    @Test
    void removeObserverFromAllWithMultipleAuctionsDoesNotThrow() {
        service.addAuction(new Auction("obs1", new Electronics("e7", "TV", PRICE), DURATION, null));
        service.addAuction(new Auction("obs2", new Electronics("e8", "Radio", PRICE), DURATION, null));
        assertDoesNotThrow(() -> service.removeObserverFromAll(msg -> {
        }));
    }
 
    // --- getAuctionsMap ---
 
    @Test
    void getAuctionsMapAfterAddContainsAuction() {
        Auction a = new Auction("map1", new Electronics("e9", "Camera", PRICE), DURATION, null);
        service.addAuction(a);
        assertTrue(service.getAuctionsMap().containsKey("map1"));
    }
 
    // --- shutdown with auctions ---
 
    @Test
    void shutdownWithActiveAuctionsDoesNotThrow() {
        service.addAuction(new Auction("shut1", new Electronics("e10", "TV", PRICE), DURATION, null));
        assertDoesNotThrow(() -> service.shutdown());
    }
}