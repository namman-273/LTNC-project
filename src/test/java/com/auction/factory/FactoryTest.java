package com.auction.factory;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.auction.model.entities.item.Art;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.item.Item;
import com.auction.model.entities.item.Vehicle;
import com.auction.model.factory.ArtFactory;
import com.auction.model.factory.ItemFactoryRegistry;
import com.auction.model.factory.ElectronicsFactory;
import com.auction.model.factory.VehicleFactory;
 
public class FactoryTest {
 
  private static final double PRICE = 500.0;
 
  // --- CreateItem.getFactory ---
 
  @Test
  void getFactoryArtReturnsArtFactory() {
    assertInstanceOf(ArtFactory.class, ItemFactoryRegistry.getFactory("ART"));
  }
 
  @Test
  void getFactoryElectronicsReturnsElectronicsFactory() {
    assertInstanceOf(ElectronicsFactory.class, ItemFactoryRegistry.getFactory("ELECTRONICS"));
  }
 
  @Test
  void getFactoryVehicleReturnsVehicleFactory() {
    assertInstanceOf(VehicleFactory.class, ItemFactoryRegistry.getFactory("VEHICLE"));
  }
 
  @Test
  void getFactoryArtLowercaseReturnsArtFactory() {
    assertInstanceOf(ArtFactory.class, ItemFactoryRegistry.getFactory("art"));
  }
 
  @Test
  void getFactoryElectronicsLowercaseReturnsElectronicsFactory() {
    assertInstanceOf(ElectronicsFactory.class, ItemFactoryRegistry.getFactory("electronics"));
  }
 
  @Test
  void getFactoryVehicleLowercaseReturnsVehicleFactory() {
    assertInstanceOf(VehicleFactory.class, ItemFactoryRegistry.getFactory("vehicle"));
  }
 
  @Test
  void getFactoryUnknownTypeReturnsVehicleFactoryAsDefault() {
    assertInstanceOf(VehicleFactory.class, ItemFactoryRegistry.getFactory("UNKNOWN"));
  }
 
  // --- ArtFactory ---
 
  @Test
  void artFactoryCreateReturnsArtInstance() {
    assertInstanceOf(Art.class, new ArtFactory().create("a1", "Mona Lisa", PRICE));
  }
 
  @Test
  void artFactoryCreateHasCorrectId() {
    Item item = new ArtFactory().create("a1", "Mona Lisa", PRICE);
    assertEquals("a1", item.getId());
  }
 
  @Test
  void artFactoryCreateHasCorrectName() {
    Item item = new ArtFactory().create("a1", "Mona Lisa", PRICE);
    assertEquals("Mona Lisa", item.getItemName());
  }
 
  @Test
  void artFactoryCreateHasCorrectStartingPrice() {
    Item item = new ArtFactory().create("a1", "Mona Lisa", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
 
  @Test
  void artFactoryCreateIsNotNull() {
    assertNotNull(new ArtFactory().create("a1", "Mona Lisa", PRICE));
  }
 
  // --- ElectronicsFactory ---
 
  @Test
  void electronicsFactoryCreateReturnsElectronicsInstance() {
    assertInstanceOf(Electronics.class, new ElectronicsFactory().create("e1", "Laptop", PRICE));
  }
 
  @Test
  void electronicsFactoryCreateHasCorrectId() {
    Item item = new ElectronicsFactory().create("e1", "Laptop", PRICE);
    assertEquals("e1", item.getId());
  }
 
  @Test
  void electronicsFactoryCreateHasCorrectName() {
    Item item = new ElectronicsFactory().create("e1", "Laptop", PRICE);
    assertEquals("Laptop", item.getItemName());
  }
 
  @Test
  void electronicsFactoryCreateHasCorrectPrice() {
    Item item = new ElectronicsFactory().create("e1", "Laptop", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
 
  // --- VehicleFactory ---
 
  @Test
  void vehicleFactoryCreateReturnsVehicleInstance() {
    assertInstanceOf(Vehicle.class, new VehicleFactory().create("v1", "Honda", PRICE));
  }
 
  @Test
  void vehicleFactoryCreateHasCorrectId() {
    Item item = new VehicleFactory().create("v1", "Honda", PRICE);
    assertEquals("v1", item.getId());
  }
 
  @Test
  void vehicleFactoryCreateHasCorrectName() {
    Item item = new VehicleFactory().create("v1", "Honda", PRICE);
    assertEquals("Honda", item.getItemName());
  }
 
  @Test
  void vehicleFactoryCreateHasCorrectPrice() {
    Item item = new VehicleFactory().create("v1", "Honda", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
}