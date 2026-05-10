package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ItemExtendedTest {

    @Test void electronicsGetters()          { Electronics e=new Electronics("e1","Laptop",10_000_000.0); assertEquals("Laptop",e.getItemName()); }
    @Test void electronicsSetCurrentPrice()  { Electronics e=new Electronics("e2","Phone",5_000_000.0); e.setCurrentPrice(6_000_000.0); assertEquals(6_000_000.0,e.getCurrentPrice(),0.001); }
    @Test void electronicsSetHighestBidder() { Electronics e=new Electronics("e3","TV",3_000_000.0); e.setHighestBidder("alice"); assertEquals("alice",e.getHighestBidder()); }
    @Test void electronicsDisplayInfo()      { assertDoesNotThrow(new Electronics("e5","Tab",1_000_000.0)::displayInfo); }
    @Test void artDisplayInfo()              { assertDoesNotThrow(new Art("a2","Painting",500_000.0)::displayInfo); }
    @Test void artSetCurrentPrice()          { Art a=new Art("a3","Sculpture",200_000.0); a.setCurrentPrice(300_000.0); assertEquals(300_000.0,a.getCurrentPrice(),0.001); }
    @Test void vehicleGetters()              { Vehicle v=new Vehicle("v1","Tesla",800_000_000.0); assertEquals("Tesla",v.getItemName()); }
    @Test void vehicleDisplayInfo()          { assertDoesNotThrow(new Vehicle("v2","BMW",1_200_000_000.0)::displayInfo); }
    @Test void vehicleSetHighestBidder()     { Vehicle v=new Vehicle("v3","Toyota",500_000_000.0); v.setHighestBidder("bob"); assertEquals("bob",v.getHighestBidder()); }
}