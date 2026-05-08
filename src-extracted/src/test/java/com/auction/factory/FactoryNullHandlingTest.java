package com.auction.factory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.auction.model.Item;
import org.junit.jupiter.api.Test;


public class FactoryNullHandlingTest {

    private static final double PRICE = 1000.0;

    // --- getFactory: empty string trả về default factory ---

    @Test
    void getFactoryEmptyStringReturnsNonNull() {
        assertNotNull(com.auction.model.CreateItem.getFactory(""));
    }

    // --- ElectronicsFactory: null/empty id/name vẫn tạo được object ---

    @Test
    void createItemWithNullIdDoesNotThrowAndReturnsItem() {
        Item item = new ElectronicsFactory().create(null, "Laptop", PRICE);
        assertNotNull(item, "Factory must return non-null item even with null id");
    }

    @Test
    void createItemWithEmptyNameReturnsItemWithEmptyName() {
        Item item = new ElectronicsFactory().create("id1", "", PRICE);
        assertNotNull(item);
        assertEquals("", item.getItemName());
    }

    @Test
    void createItemWithNullNameReturnsItemWithNullName() {
        Item item = new ElectronicsFactory().create("id2", null, PRICE);
        assertNotNull(item);
        assertDoesNotThrow(() -> item.getItemName()); // null là OK, không nên throw
    }

    @Test
    void createItemWithZeroPriceHasZeroStartingPrice() {
        Item item = new ElectronicsFactory().create("id3", "TV", 0.0);
        assertNotNull(item);
        assertEquals(0.0, item.getStartingPrice(), 0.001);
    }

    @Test
    void createItemWithNegativePricePreservesNegativePrice() {
        Item item = new ElectronicsFactory().create("id4", "TV", -1.0);
        assertNotNull(item);
        assertEquals(-1.0, item.getStartingPrice(), 0.001,
            "Factory stores price as-is; validation is the caller's responsibility");
    }

    // --- ArtFactory ---

    @Test
    void artFactoryNullIdReturnsItemWithCorrectName() {
        Item item = new ArtFactory().create(null, "Painting", PRICE);
        assertNotNull(item);
        assertEquals("Painting", item.getItemName());
    }

    @Test
    void artFactoryNullNameReturnsItemWithNullName() {
        Item item = new ArtFactory().create("id5", null, PRICE);
        assertNotNull(item);
    }

    @Test
    void artFactoryZeroPriceHasZeroStartingPrice() {
        Item item = new ArtFactory().create("id6", "Vase", 0.0);
        assertEquals(0.0, item.getStartingPrice(), 0.001);
    }

    // --- VehicleFactory ---

    @Test
    void vehicleFactoryNullIdReturnsItemWithCorrectName() {
        Item item = new VehicleFactory().create(null, "Honda", PRICE);
        assertNotNull(item);
        assertEquals("Honda", item.getItemName());
    }

    @Test
    void vehicleFactoryNullNameReturnsItem() {
        Item item = new VehicleFactory().create("id7", null, PRICE);
        assertNotNull(item);
    }

    @Test
    void vehicleFactoryZeroPriceHasZeroStartingPrice() {
        Item item = new VehicleFactory().create("id8", "Car", 0.0);
        assertEquals(0.0, item.getStartingPrice(), 0.001);
    }
}