package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.dto.AuctionRow;
import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.item.Item;
import com.auction.model.enums.AuctionStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for AuctionRow DTO – sử dụng constructor Auction (đã refactor).
 */
public class AuctionRowTest {

    private static final String ITEM_NAME = "Laptop";
    private static final double PRICE = 1_500_000.0;
    private static final String SELLER_ID = "seller1";
    private static final long DURATION = 9999L;

    private Auction auction;
    private AuctionRow row;

    @BeforeEach
    void setUp() {
        Item item = new Electronics("item-001", ITEM_NAME, PRICE);
        auction = new Auction("AUC_001", item, DURATION, SELLER_ID);
        row = new AuctionRow(auction);
    }

    // ===== getId =====

    @Test
    void getIdReturnsAuctionId() {
        assertEquals("AUC_001", row.getId());
    }

    // ===== getItemName =====

    @Test
    void getItemNameReturnsCorrectName() {
        assertEquals(ITEM_NAME, row.getItemName());
    }

    // ===== getCurrentPrice =====

    @Test
    void getCurrentPriceReturnsStartingPrice() {
        assertEquals(PRICE, row.getCurrentPrice(), 0.001);
    }

    // ===== getStatus =====

    @Test
    void getStatusReturnsOpenForNewAuction() {
        assertEquals(AuctionStatus.OPEN.toString(), row.getStatus());
    }

    // ===== getEndTime =====

    @Test
    void getEndTimeIsInFuture() {
        assertTrue(row.getEndTime() > System.currentTimeMillis());
    }

    // ===== getSellerId =====

    @Test
    void getSellerIdReturnsCorrectSellerId() {
        assertEquals(SELLER_ID, row.getSellerId());
    }

    @Test
    void getSellerIdNullSellerBecomesAnonymous() {
        Item item = new Electronics("item-002", "Phone", PRICE);
        Auction auctionNoSeller = new Auction("AUC_002", item, DURATION, null);
        AuctionRow rowNoSeller = new AuctionRow(auctionNoSeller);
        assertEquals("Anonymous", rowNoSeller.getSellerId());
    }

    @Test
    void getSellerIdEmptySellerBecomesAnonymous() {
        Item item = new Electronics("item-003", "Watch", PRICE);
        Auction auctionEmptySeller = new Auction("AUC_003", item, DURATION, "");
        AuctionRow rowEmpty = new AuctionRow(auctionEmptySeller);
        assertEquals("Anonymous", rowEmpty.getSellerId());
    }

    // ===== getCurrentPriceFormatted =====

    @Test
    void getCurrentPriceFormattedIsNotNull() {
        assertNotNull(row.getCurrentPriceFormatted());
    }

    @Test
    void getCurrentPriceFormattedContainsVND() {
        assertTrue(row.getCurrentPriceFormatted().contains("VNĐ"));
    }

    @Test
    void getCurrentPriceFormattedContainsNumericValue() {
        Item item = new Electronics("item-004", "TV", 500_000.0);
        Auction a = new Auction("AUC_004", item, DURATION, SELLER_ID);
        AuctionRow r = new AuctionRow(a);
        String formatted = r.getCurrentPriceFormatted();
        assertTrue(formatted.contains("500"), "Formatted price must contain '500'");
    }

    @Test
    void zeroCurrentPriceFormatted() {
        Item item = new Electronics("item-005", "Freebie", 0.0);
        Auction a = new Auction("AUC_005", item, DURATION, SELLER_ID);
        AuctionRow r = new AuctionRow(a);
        String formatted = r.getCurrentPriceFormatted();
        assertNotNull(formatted);
        assertTrue(formatted.contains("VNĐ"));
    }

    // ===== status reflects auction state =====

    @Test
    void statusIsRunningWhenAuctionIsRunning() {
        auction.setStatus(AuctionStatus.RUNNING);
        AuctionRow r = new AuctionRow(auction);
        assertEquals(AuctionStatus.RUNNING.toString(), r.getStatus());
    }

}