package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.dto.AuctionRow;

import org.junit.jupiter.api.Test;

/**
 * Tests for AuctionRow DTO – both constructors and all getters.
 */
public class AuctionRowTest {

    private static final String ID = "AUC_001";
    private static final String ITEM_NAME = "Laptop";
    private static final double PRICE = 1_500_000.0;
    private static final String STATUS = "OPEN";
    private static final long END_TIME = 9_999_999_999L;
    private static final String SELLER_ID = "seller1";

    // ===== 5-arg constructor =====

    @Test
    void fiveArgConstructorGetId() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertEquals(ID, row.getId());
    }

    @Test
    void fiveArgConstructorGetItemName() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertEquals(ITEM_NAME, row.getItemName());
    }

    @Test
    void fiveArgConstructorGetCurrentPrice() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertEquals(PRICE, row.getCurrentPrice(), 0.001);
    }

    @Test
    void fiveArgConstructorGetStatus() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertEquals(STATUS, row.getStatus());
    }

    @Test
    void fiveArgConstructorGetEndTime() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertEquals(END_TIME, row.getEndTime());
    }

    @Test
    void fiveArgConstructorSellerIdIsEmptyString() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertEquals("", row.getSellerId());
    }

    // ===== 6-arg constructor =====

    @Test
    void sixArgConstructorGetSellerId() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME, SELLER_ID);
        assertEquals(SELLER_ID, row.getSellerId());
    }

    @Test
    void sixArgConstructorNullSellerIdBecomesEmpty() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME, null);
        assertEquals("", row.getSellerId());
    }

    @Test
    void sixArgConstructorAllFieldsCorrect() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME, SELLER_ID);
        assertEquals(ID, row.getId());
        assertEquals(ITEM_NAME, row.getItemName());
        assertEquals(PRICE, row.getCurrentPrice(), 0.001);
        assertEquals(STATUS, row.getStatus());
        assertEquals(END_TIME, row.getEndTime());
        assertEquals(SELLER_ID, row.getSellerId());
    }

    // ===== getCurrentPriceFormatted =====

    @Test
    void getCurrentPriceFormattedIsNotNull() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertNotNull(row.getCurrentPriceFormatted());
    }

    @Test
    void getCurrentPriceFormattedContainsVND() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, PRICE, STATUS, END_TIME);
        assertTrue(row.getCurrentPriceFormatted().contains("VNĐ"));
    }

    @Test
    void getCurrentPriceFormattedContainsNumericValue() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, 500_000.0, STATUS, END_TIME);
        // Should contain 500000 or 500,000 depending on locale
        String formatted = row.getCurrentPriceFormatted();
        assertTrue(formatted.contains("500"), "Formatted price must contain '500'");
    }

    @Test
    void zeroCurrentPriceFormatted() {
        AuctionRow row = new AuctionRow(ID, ITEM_NAME, 0.0, STATUS, END_TIME);
        String formatted = row.getCurrentPriceFormatted();
        assertNotNull(formatted);
        assertTrue(formatted.contains("VNĐ"));
    }
}