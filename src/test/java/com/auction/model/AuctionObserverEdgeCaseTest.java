package com.auction.model;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.service.UserManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class AuctionObserverEdgeCaseTest {
 
    private static final double STARTING_PRICE = 1000.0;
    private static final double BID_1 = 51000.0;
    private static final long DURATION = 9999L;
    private static final long ASYNC_WAIT_MS = 200L;
 
    private Auction auction;
    private Bidder bidder1;
 
    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);
        UserManager.getInstance().register("alice", "pw", "BIDDER");
        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder1.addBalance(10_000_000.0);
 
        Item item = new Electronics("item-o", "TV", STARTING_PRICE);
        auction = new Auction("auction-o", item, DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
    }
 
    @Test
    void twoObserversBothReceiveNotification() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        auction.addObserver(msg -> count.incrementAndGet());
        auction.addObserver(msg -> count.incrementAndGet());
 
        auction.notifyObservers("ping");
        Thread.sleep(ASYNC_WAIT_MS);
 
        assertEquals(2, count.get(), "Both observers must receive the notification");
    }
 
    @Test
    void notificationContainsBidAmount() throws Exception {
        List<String> received = new ArrayList<>();
        auction.addObserver(msg -> received.add(msg));
 
        auction.processNewBid(bidder1, BID_1);
        Thread.sleep(ASYNC_WAIT_MS);
 
        assertTrue(received.stream().anyMatch(m -> m.contains(String.valueOf(BID_1))),
            "Notification must contain the bid amount");
    }
 
    @Test
    void notificationContainsBidderUsername() throws Exception {
        List<String> received = new ArrayList<>();
        auction.addObserver(msg -> received.add(msg));
 
        auction.processNewBid(bidder1, BID_1);
        Thread.sleep(ASYNC_WAIT_MS);
 
        assertTrue(received.stream().anyMatch(m -> m.contains("alice")),
            "Notification must contain the bidder username");
    }
 
    @Test
    void removedObserverReceivesNoMessage() throws Exception {
        List<String> received = new ArrayList<>();
        Observer obs = msg -> received.add(msg);
        auction.addObserver(obs);
        auction.removeObserver(obs);
 
        auction.processNewBid(bidder1, BID_1);
        Thread.sleep(ASYNC_WAIT_MS);
 
        assertTrue(received.isEmpty(), "Removed observer must not receive any message");
    }
 
    @Test
    void noObserversNotifyDoesNotThrow() {
        assertDoesNotThrow(() -> auction.notifyObservers("msg"),
            "notifyObservers with no observers must not throw");
    }
 
    @Test
    void observerNotNotifiedAfterCloseAuction() throws Exception {
        List<String> received = new ArrayList<>();
        auction.addObserver(msg -> received.add(msg));
 
        auction.closeAuction();
        auction.notifyObservers("after-close");
        Thread.sleep(ASYNC_WAIT_MS);
 
        assertTrue(received.isEmpty(),
            "No message must be delivered after closeAuction");
    }
 
    @Test
    void partialRemoveOnlyRemovedObserverStops() throws Exception {
        AtomicInteger countA = new AtomicInteger(0);
        AtomicInteger countB = new AtomicInteger(0);
        Observer obsA = msg -> countA.incrementAndGet();
        Observer obsB = msg -> countB.incrementAndGet();
 
        auction.addObserver(obsA);
        auction.addObserver(obsB);
        auction.removeObserver(obsA);
 
        auction.notifyObservers("test");
        Thread.sleep(ASYNC_WAIT_MS);
 
        assertEquals(0, countA.get(), "Removed obsA must not receive message");
        assertEquals(1, countB.get(), "Kept obsB must receive message");
    }
}