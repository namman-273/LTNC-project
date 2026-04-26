package com.auction.factory;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
 
import com.auction.model.Art;
import com.auction.model.CreateItem;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;
import org.junit.jupiter.api.Test;
 
public class FactoryTest {
 
  private static final double PRICE = 500.0;
 
  // ================================================================
  // CreateItem — getFactory
  // ================================================================
 
  @Test
  void getFactoryArtReturnsArtFactory() {
    assertInstanceOf(ArtFactory.class, CreateItem.getFactory("ART"));
  }
 
  @Test
  void getFactoryElectronicsReturnsElectronicsFactory() {
    assertInstanceOf(ElectronicsFactory.class, CreateItem.getFactory("ELECTRONICS"));
  }
 
  @Test
  void getFactoryVehicleReturnsVehicleFactory() {
    assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("VEHICLE"));
  }
 
  @Test
  void getFactoryCaseInsensitiveArtReturnsArtFactory() {
    assertInstanceOf(ArtFactory.class, CreateItem.getFactory("art"));
  }
 
  @Test
  void getFactoryCaseInsensitiveElectronicsReturnsElectronicsFactory() {
    assertInstanceOf(ElectronicsFactory.class, CreateItem.getFactory("electronics"));
  }
 
  @Test
  void getFactoryUnknownTypeReturnsVehicleFactoryAsDefault() {
    assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("UNKNOWN"));
  }
 
  // ================================================================
  // ArtFactory
  // ================================================================
 
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
 
  // ================================================================
  // ElectronicsFactory
  // ================================================================
 
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
 
  // ================================================================
  // VehicleFactory
  // ================================================================
 
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
}