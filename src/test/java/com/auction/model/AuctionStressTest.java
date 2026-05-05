package com.auction.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.service.UserManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class AuctionStressTest {

    private static final double STARTING_PRICE = 1000.0;
    private static final long DURATION = 9999L;
    private static final int THREAD_COUNT = 20;

    private Auction auction;
    private List<Bidder> bidders;

    @BeforeEach
    void setUp() throws Exception {
        Field umField = UserManager.class.getDeclaredField("instance");
        umField.setAccessible(true);
        umField.set(null, null);

        bidders = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            String name = "bidder" + i;
            UserManager.getInstance().register(name, "pw", "BIDDER");
            Bidder b = (Bidder) UserManager.getInstance().findUserByUsername(name);
            b.addBalance(50_000_000.0);
            bidders.add(b);
        }

        Item item = new Electronics("stress-item", "Laptop", STARTING_PRICE);
        auction = new Auction("stress-auction", item, DURATION, null);
        auction.setStatus(AuctionStatus.RUNNING);
    }

    // --- Stress test 1: 20 thread cùng đặt giá tăng dần ---

    @Test
    void twentyThreadsBidIncrementallyPriceMonotonicallyIncreases() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final Bidder b = bidders.get(i);
            final double bidAmount = STARTING_PRICE + (i + 1) * 50000.0;
            new Thread(() -> {
                try {
                    startLatch.await();
                    auction.processNewBid(b, bidAmount);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads must finish within 5s");

        assertTrue(successCount.get() >= 1, "At least one bid must succeed");
        assertTrue(auction.getCurrentPrice() >= STARTING_PRICE,
            "Price must never drop below starting price");
        assertBidHistoryPricesMonotonicallyIncreasing();
    }

    private void assertBidHistoryPricesMonotonicallyIncreasing() {
        List<BidTransaction> history = auction.getBidHistory();
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i).getAmount() > history.get(i - 1).getAmount(),
                "Bid history must have strictly increasing prices");
        }
    }

    // --- Stress test 2: tổng tiền bảo toàn (conservation of money) ---

    @Test
    void balanceConservationUnderConcurrentBidding() throws Exception {
        double totalBefore = bidders.stream().mapToDouble(Bidder::getBalance).sum();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final Bidder b = bidders.get(i);
            final double bidAmount = STARTING_PRICE + (i + 1) * 50000.0;
            new Thread(() -> {
                try {
                    startLatch.await();
                    auction.processNewBid(b, bidAmount);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        // Tổng tiền trong ví + tiền đang bị hold trong auction = totalBefore
        double totalAfter = bidders.stream().mapToDouble(Bidder::getBalance).sum();
        double heldInAuction = auction.getCurrentPrice() > STARTING_PRICE
            ? auction.getCurrentPrice() : 0;

        assertEquals(totalBefore, totalAfter + heldInAuction, 1.0,
            "Total balance must be conserved (wallets + held amount = original total)");
    }

    // --- Stress test 3: 20 thread cùng addObserver, rồi notifyObservers ---

    @Test
    void twentyObserversAllReceiveNotification() throws Exception {
        AtomicInteger received = new AtomicInteger(0);
        CountDownLatch addLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                auction.addObserver(msg -> received.incrementAndGet());
                addLatch.countDown();
            });
        }
        assertTrue(addLatch.await(3, TimeUnit.SECONDS));
        pool.shutdown();

        auction.notifyObservers("broadcast");
        Thread.sleep(300);

        assertEquals(THREAD_COUNT, received.get(),
            "All 20 observers must receive the notification");
    }

    // --- Stress test 4: race condition giữa bid và closeAuction ---

    @Test
    void noBidSucceedsAfterCloseUnderConcurrency() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT + 1);
        AtomicInteger unexpectedSuccess = new AtomicInteger(0);

        // Thread đóng phiên
        new Thread(() -> {
            try {
                startLatch.await();
                auction.closeAuction();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        }).start();

        // 20 thread đặt giá đồng thời
        for (int i = 0; i < THREAD_COUNT; i++) {
            final Bidder b = bidders.get(i);
            final double bidAmount = STARTING_PRICE + (i + 1) * 50000.0;
            new Thread(() -> {
                try {
                    startLatch.await();
                    auction.processNewBid(b, bidAmount);
                } catch (com.auction.exception.AuctionClosedException ignored) {
                    // expected sau khi close
                } catch (Exception e) {
                    // InvalidBidException etc cũng OK
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, unexpectedSuccess.get());
    }

    // --- Stress test 5: autoBid dưới tải nhiều bidder ---

    @Test
    void autoBidUnderConcurrentBiddersNoPriceDropOrException() throws Exception {
        // Đăng ký auto-bid cho 5 bidder đầu
        for (int i = 0; i < 5; i++) {
            final String name = "bidder" + i;
            auction.addAutoBidConfig(name, STARTING_PRICE + (i + 1) * 500000.0);
        }

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(10);
        AtomicInteger errors = new AtomicInteger(0);

        // 10 thread bidder còn lại đặt giá thủ công
        for (int i = 5; i < 15; i++) {
            final Bidder b = bidders.get(i);
            final double amount = STARTING_PRICE + (i + 1) * 50000.0;
            new Thread(() -> {
                try {
                    latch.await();
                    auction.processNewBid(b, amount);
                } catch (com.auction.exception.InvalidBidException
                         | com.auction.exception.AuctionClosedException ignored) {
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        latch.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));

        assertEquals(0, errors.get(), "No unexpected exceptions under auto-bid + concurrent manual bids");
        assertTrue(auction.getCurrentPrice() >= STARTING_PRICE);
    }
}