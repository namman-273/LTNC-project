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
 
  // ----------------------------------------------------------------
  // CreateItem — getFactory (selector đúng loại)
  // ----------------------------------------------------------------
 
  @Test
  void testGetFactory_art_returnsArtFactory() {
    assertInstanceOf(ArtFactory.class, CreateItem.getFactory("ART"));
  }
 
  @Test
  void testGetFactory_electronics_returnsElectronicsFactory() {
    assertInstanceOf(ElectronicsFactory.class, CreateItem.getFactory("ELECTRONICS"));
  }
 
  @Test
  void testGetFactory_vehicle_returnsVehicleFactory() {
    assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("VEHICLE"));
  }
 
  @Test
  void testGetFactory_caseInsensitiveArt_returnsArtFactory() {
    assertInstanceOf(ArtFactory.class, CreateItem.getFactory("art"));
  }
 
  @Test
  void testGetFactory_caseInsensitiveElectronics_returnsElectronicsFactory() {
    assertInstanceOf(ElectronicsFactory.class, CreateItem.getFactory("electronics"));
  }
 
  @Test
  void testGetFactory_unknownType_returnsVehicleFactoryAsDefault() {
    assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("UNKNOWN"));
  }
 
  // ----------------------------------------------------------------
  // ArtFactory — create
  // ----------------------------------------------------------------
 
  @Test
  void testArtFactory_create_returnsArtInstance() {
    assertInstanceOf(Art.class, new ArtFactory().create("a1", "Mona Lisa", PRICE));
  }
 
  @Test
  void testArtFactory_create_hasCorrectId() {
    Item item = new ArtFactory().create("a1", "Mona Lisa", PRICE);
    assertEquals("a1", item.getId());
  }
 
  @Test
  void testArtFactory_create_hasCorrectName() {
    Item item = new ArtFactory().create("a1", "Mona Lisa", PRICE);
    assertEquals("Mona Lisa", item.getItemName());
  }
 
  @Test
  void testArtFactory_create_hasCorrectStartingPrice() {
    Item item = new ArtFactory().create("a1", "Mona Lisa", PRICE);
    assertEquals(PRICE, item.getStartingPrice());
  }
 
  // ----------------------------------------------------------------
  // ElectronicsFactory — create
  // ----------------------------------------------------------------
 
  @Test
  void testElectronicsFactory_create_returnsElectronicsInstance() {
    assertInstanceOf(Electronics.class, new ElectronicsFactory().create("e1", "Laptop", PRICE));
  }
 
  @Test
  void testElectronicsFactory_create_hasCorrectId() {
    Item item = new ElectronicsFactory().create("e1", "Laptop", PRICE);
    assertEquals("e1", item.getId());
  }
 
  @Test
  void testElectronicsFactory_create_hasCorrectName() {
    Item item = new ElectronicsFactory().create("e1", "Laptop", PRICE);
    assertEquals("Laptop", item.getItemName());
  }
 
  // ----------------------------------------------------------------
  // VehicleFactory — create
  // ----------------------------------------------------------------
 
  @Test
  void testVehicleFactory_create_returnsVehicleInstance() {
    assertInstanceOf(Vehicle.class, new VehicleFactory().create("v1", "Honda", PRICE));
  }
 
  @Test
  void testVehicleFactory_create_hasCorrectId() {
    Item item = new VehicleFactory().create("v1", "Honda", PRICE);
    assertEquals("v1", item.getId());
  }
 
  @Test
  void testVehicleFactory_create_hasCorrectName() {
    Item item = new VehicleFactory().create("v1", "Honda", PRICE);
    assertEquals("Honda", item.getItemName());
  }
}