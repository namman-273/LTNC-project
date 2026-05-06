package com.auction.util;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.model.Auction;
import com.auction.model.Electronics;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;
 
public class DataManagerTest {
 
    private static final long DURATION = 9999L;
    private static final double PRICE = 500.0;
 
    @BeforeEach
    void setUp() throws Exception {
        Field dmField = DataManager.class.getDeclaredField("instance");
        dmField.setAccessible(true);
        dmField.set(null, null);
 
        Field asField = AuctionService.class.getDeclaredField("instance");
        asField.setAccessible(true);
        asField.set(null, null);
 
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
 
        UserManager.getInstance().register("alice", "pw", "BIDDER");
    }
 
    @AfterEach
    void tearDown() {
        new File("auctions.dat").delete();
        new File("users.dat").delete();
        new File("auctions.dat.tmp").delete();
        new File("users.dat.tmp").delete();
    }
 
    // --- Singleton ---
 
   
 
    @Test
    void saveDataDoesNotThrow() {
        AuctionService.getInstance().addAuction(
            new Auction("a1", new Electronics("e1", "Laptop", PRICE), DURATION, null));
        assertDoesNotThrow(() -> DataManager.getInstance().saveData());
    }
 
    @Test
    void saveDataCreatesAuctionFile() {
        AuctionService.getInstance().addAuction(
            new Auction("a2", new Electronics("e2", "TV", PRICE), DURATION, null));
        DataManager.getInstance().saveData();
        assertTrue(new File("auctions.dat").exists(),
            "auctions.dat must be created after saveData");
    }
 
    
 
    @Test
    void saveDataCalledTwiceDoesNotThrow() {
        AuctionService.getInstance().addAuction(
            new Auction("a3", new Electronics("e3", "Phone", PRICE), DURATION, null));
        assertDoesNotThrow(() -> {
            DataManager.getInstance().saveData();
            DataManager.getInstance().saveData();
        });
    }
 
    // --- loadData ---
 
    @Test
    void loadDataWhenNoFilesDoesNotThrow() {
        assertDoesNotThrow(() -> DataManager.getInstance().loadData());
    }
 
   
 
    
 
   
}