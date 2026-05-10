package com.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import com.auction.model.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuctionServiceTest {

    @BeforeEach
    void reset() throws Exception {
        Field asf = AuctionService.class.getDeclaredField("instance");
        asf.setAccessible(true);
        asf.set(null, null);
        Field umf = UserManager.class.getDeclaredField("instance");
        umf.setAccessible(true);
        umf.set(null, null);
        UserManager.getInstance().register("seller1", "pw", "SELLER");
        UserManager.getInstance().register("buyer1",  "pw", "BIDDER");
        ((Bidder) UserManager.getInstance().findUserByUsername("buyer1")).addBalance(100_000_000.0);
    }

    @Test void createAddsToMap() {
        AuctionService svc = AuctionService.getInstance();
        int before = svc.getAllAuctions().size();
        svc.createNewAuction("ELECTRONICS","Phone",1_000_000.0,60L,"seller1");
        assertEquals(before + 1, svc.getAllAuctions().size());
    }

    @Test void getByIdUnknownNull()    { assertNull(AuctionService.getInstance().getAuctionById("NONE")); }
    @Test void deleteNonExistentFalse(){ assertFalse(AuctionService.getInstance().deleteAuction("GHOST")); }
    @Test void endUnknownDoesNotThrow(){ assertDoesNotThrow(() -> AuctionService.getInstance().endAuction("GHOST")); }
    @Test void singletonSameInstance() { assertSame(AuctionService.getInstance(), AuctionService.getInstance()); }
    @Test void getAuctionsMapNotNull() { assertNotNull(AuctionService.getInstance().getAuctionsMap()); }

    @Test void deleteReturnsTrueAndRemoves() {
        AuctionService svc = AuctionService.getInstance();
        svc.createNewAuction("ELECTRONICS","TV",3_000_000.0,60L,"seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        assertTrue(svc.deleteAuction(id));
        assertNull(svc.getAuctionById(id));
    }

    @Test void endAuctionSetsFinished() {
        AuctionService svc = AuctionService.getInstance();
        svc.createNewAuction("ELECTRONICS","Tablet",2_000_000.0,60L,"seller1");
        String id = svc.getAllAuctions().iterator().next().getId();
        Auction a = svc.getAuctionById(id);
        a.setStatus(AuctionStatus.OPEN);
        svc.endAuction(id);
        assertTrue(a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID);
    }
}