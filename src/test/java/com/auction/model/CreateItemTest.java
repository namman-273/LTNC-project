package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
 
import com.auction.factory.ArtFactory;
import com.auction.factory.ElectronicsFactory;
import com.auction.factory.ItemFactory;
import com.auction.factory.VehicleFactory;
import org.junit.jupiter.api.Test;
 
public class CreateItemTest {
 
    private static final double PRICE = 500.0;
 
    // --- CreateItem.getFactory ---
 
    @Test
    void getFactoryArtUppercaseReturnsArtFactory() {
        assertInstanceOf(ArtFactory.class, CreateItem.getFactory("ART"));
    }
 
    @Test
    void getFactoryArtLowercaseReturnsArtFactory() {
        assertInstanceOf(ArtFactory.class, CreateItem.getFactory("art"));
    }
 
    @Test
    void getFactoryArtMixedCaseReturnsArtFactory() {
        assertInstanceOf(ArtFactory.class, CreateItem.getFactory("Art"));
    }
 
    @Test
    void getFactoryElectronicsUppercaseReturnsElectronicsFactory() {
        assertInstanceOf(ElectronicsFactory.class, CreateItem.getFactory("ELECTRONICS"));
    }
 
    @Test
    void getFactoryElectronicsLowercaseReturnsElectronicsFactory() {
        assertInstanceOf(ElectronicsFactory.class, CreateItem.getFactory("electronics"));
    }
 
    @Test
    void getFactoryVehicleUppercaseReturnsVehicleFactory() {
        assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("VEHICLE"));
    }
 
    @Test
    void getFactoryVehicleLowercaseReturnsVehicleFactory() {
        assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("vehicle"));
    }
 
    @Test
    void getFactoryUnknownTypeReturnsVehicleFactoryAsDefault() {
        assertInstanceOf(VehicleFactory.class, CreateItem.getFactory("UNKNOWN"));
    }
 
    @Test
    void getFactoryEmptyStringReturnsVehicleFactoryAsDefault() {
        assertInstanceOf(VehicleFactory.class, CreateItem.getFactory(""));
    }
 
    // --- Factory creates correct Item types ---
 
    @Test
    void artFactoryCreateReturnsArtItem() {
        ItemFactory factory = CreateItem.getFactory("ART");
        Item item = factory.create("id1", "Painting", PRICE);
        assertInstanceOf(Art.class, item);
    }
 
    @Test
    void electronicsFactoryCreateReturnsElectronicsItem() {
        ItemFactory factory = CreateItem.getFactory("ELECTRONICS");
        Item item = factory.create("id2", "Laptop", PRICE);
        assertInstanceOf(Electronics.class, item);
    }
 
    @Test
    void vehicleFactoryCreateReturnsVehicleItem() {
        ItemFactory factory = CreateItem.getFactory("VEHICLE");
        Item item = factory.create("id3", "Honda", PRICE);
        assertInstanceOf(Vehicle.class, item);
    }
 
    @Test
    void factoryCreatedItemNotNull() {
        Item item = CreateItem.getFactory("ART").create("id4", "Vase", PRICE);
        assertNotNull(item);
    }
 
    @Test
    void factoryCreatedItemHasCorrectName() {
        Item item = CreateItem.getFactory("ELECTRONICS").create("id5", "Phone", PRICE);
        assertEquals("Phone", item.getItemName());
    }
 
    @Test
    void factoryCreatedItemHasCorrectPrice() {
        Item item = CreateItem.getFactory("VEHICLE").create("id6", "Car", PRICE);
        assertEquals(PRICE, item.getStartingPrice());
    }
 
    // --- Entity base methods ---
 
    @Test
    void entityGetIdReturnsCorrectId() {
        Item item = new Electronics("entity-01", "TV", PRICE);
        assertEquals("entity-01", item.getId());
    }
 
    @Test
    void entitySetIdUpdatesId() {
        Item item = new Electronics("old-id", "TV", PRICE);
        item.setId("new-id");
        assertEquals("new-id", item.getId());
    }
 
    @Test
    void entityDisplayInfoDoesNotThrow() {
        Item item = new Electronics("e1", "TV", PRICE);
        assertDoesNotThrow(item::displayInfo);
    }
 
    @Test
    void artDisplayInfoDoesNotThrow() {
        Item item = new Art("a1", "Painting", PRICE);
        assertDoesNotThrow(item::displayInfo);
    }
 
    @Test
    void vehicleDisplayInfoDoesNotThrow() {
        Item item = new Vehicle("v1", "Car", PRICE);
        assertDoesNotThrow(item::displayInfo);
    }
}