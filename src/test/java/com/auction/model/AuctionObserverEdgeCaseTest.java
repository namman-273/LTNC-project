package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.item.Item;
import com.auction.model.entities.user.Bidder;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.observer.Observer;
import com.auction.service.usermanger.UserManager;
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
        // Holder idiom: clear users via setUsers() thay vì reflection.
        UserManager.getInstance().setUsers(new java.util.HashMap<>());
        UserManager.getInstance().register("alice", "pw", "BIDDER", "alice@test.com");
        bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
        bidder1.addBalance(10_000_000.0);

        Item item = new Electronics("item-o", "TV", STARTING_PRICE);
        auction = new Auction("auction-o", item, DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
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

}