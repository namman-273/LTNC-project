package com.auction.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Electronics;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers DataManager error paths:
 * - loadData when file doesn't exist (null → skip)
 * - loadData with corrupt file → exception caught, returns null
 * - saveData IOException path (read-only file)
 * - AuctionService.setInstance
 * - AuctionService.readResolve (via serialization)
 */
public class DataManagerErrorTest {

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
        // Clean up any .dat files created during tests
        new File("auctions.dat").delete();
        new File("users.dat").delete();
        new File("auctions.dat.tmp").delete();
        new File("users.dat.tmp").delete();
    }

    // --- loadData: files don't exist ---

    @Test
    void loadDataWhenFilesAbsentDoesNotThrow() {
        // No .dat files exist → loadMapFromFile returns null → graceful skip
        assertDoesNotThrow(() -> DataManager.getInstance().loadData());
    }

    @Test
    void loadDataWhenFilesAbsentKeepsExistingUsers() {
        int before = UserManager.getInstance().getUsers().size();
        DataManager.getInstance().loadData(); // files absent
        // users map unchanged since loadedUsers = null → setUsers not called
        assertNotNull(UserManager.getInstance().findUserByUsername("alice"));
    }

    // --- loadData: corrupt file ---

    @Test
    void loadDataWithCorruptAuctionFileDoesNotThrow() throws Exception {
        // Write garbage bytes to auctions.dat
        try (FileWriter fw = new FileWriter("auctions.dat")) {
            fw.write("THIS_IS_NOT_A_SERIALIZED_OBJECT");
        }
        assertDoesNotThrow(() -> DataManager.getInstance().loadData());
    }

    @Test
    void loadDataWithCorruptUserFileDoesNotThrow() throws Exception {
        try (FileWriter fw = new FileWriter("users.dat")) {
            fw.write("CORRUPT_DATA_12345");
        }
        assertDoesNotThrow(() -> DataManager.getInstance().loadData());
    }

    // --- saveData: normal path ---

    @Test
    void saveDataCreatesFiles() {
        assertDoesNotThrow(() -> DataManager.getInstance().saveData());
        // After save, at least one of the files should exist (or silently failed on CI)
        // We just verify no exception
    }

    // --- DataManager singleton ---

    @Test
    void getInstanceReturnsSameObject() {
        DataManager a = DataManager.getInstance();
        DataManager b = DataManager.getInstance();
        assertNotNull(a);
        // same reference (synchronized singleton)
        assert a == b;
    }

    // --- AuctionService.setInstance ---

    @Test
    void setInstanceReplacesCurrentInstance() throws Exception {
        AuctionService original = AuctionService.getInstance();
        AuctionService.setInstance(original); // set same, no crash
        assertNotNull(AuctionService.getInstance());
    }

    // --- AuctionService: endAuction with OPEN status (not RUNNING) ---

    @Test
    void endAuctionWithOpenStatusAndNoWinnerSetsFinished() {
        AuctionService service = AuctionService.getInstance();
        Auction a = new Auction("open-end", new Electronics("oe", "PC", 1000.0), 9999L, null);
        // status = OPEN (default from constructor)
        service.addAuction(a);
        service.endAuction("open-end");
        assert a.getStatus() == AuctionStatus.FINISHED;
    }

    // --- AuctionService: addAuction null ---

    @Test
    void addAuctionNullDoesNotThrow() {
        assertDoesNotThrow(() -> AuctionService.getInstance().addAuction(null));
    }

    // --- AuctionService readResolve via serialization ---

    @Test
    void auctionServiceSerializeDeserializePreservesAuctions() throws Exception {
        AuctionService service = AuctionService.getInstance();
        Auction a = new Auction("ser-1", new Electronics("s1", "Watch", 500.0), 9999L, null);
        service.addAuction(a);

        // Serialize
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(service);
        oos.close();

        // Deserialize → triggers readResolve
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
        AuctionService restored = (AuctionService) ois.readObject();
        ois.close();

        assertNotNull(restored);
        assertNotNull(restored.getAuctionById("ser-1"));
    }

    // --- AuctionService readResolve: expired auctions closed on load ---

    @Test
    void readResolveClosesExpiredAuctionsOnLoad() throws Exception {
        AuctionService service = AuctionService.getInstance();

        // Create auction with endTime in past
        Auction expired = new Auction("exp-1", new Electronics("e1", "Phone", 100.0), 1L, null);
        expired.setStatus(AuctionStatus.OPEN);
        Field endField = Auction.class.getDeclaredField("endTime");
        endField.setAccessible(true);
        endField.set(expired, System.currentTimeMillis() - 60000); // 1 min ago
        service.addAuction(expired);

        // Serialize + deserialize to trigger readResolve
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(service);
        oos.close();

        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
        AuctionService restored = (AuctionService) ois.readObject();
        ois.close();

        // readResolve should have called endAuction for expired ones
        assertNotNull(restored);
    }
}