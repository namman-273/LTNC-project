package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.entities.Auction;
import com.auction.model.entities.BidTransaction;
import com.auction.model.entities.item.Electronics;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.auctionhelpers.AuctionFinancialProcessor;
import com.auction.service.usermanger.UserManager;
import com.auction.util.exception.InvalidBidException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kiểm tra AuctionFinancialProcessor.processTransaction:
 * - seller tự bid
 * - NaN / Infinite
 * - số dư không đủ
 * - bid thành công (lần đầu, có previous bidder, same bidder)
 * - refund previous bidder chính xác
 */
public class AuctionFinancialProcessorTest {

  private AuctionFinancialProcessor processor;
  private Auction auction;
  private Bidder bidder1;
  private Bidder bidder2;
  private Seller seller;

  @BeforeEach
  void setUp() throws Exception {
    // Holder idiom: clear users via setUsers() thay vì reflection.
    UserManager.getInstance().setUsers(new java.util.HashMap<>());
    UserManager.getInstance().register("seller", "pw", "SELLER", "seller@test.com");
    UserManager.getInstance().register("alice", "pw", "BIDDER", "alice@test.com");
    UserManager.getInstance().register("bob", "pw", "BIDDER", "bob@test.com");

    seller = (Seller) UserManager.getInstance().findUserByUsername("seller");
    bidder1 = (Bidder) UserManager.getInstance().findUserByUsername("alice");
    bidder2 = (Bidder) UserManager.getInstance().findUserByUsername("bob");

    bidder1.addBalance(10_000_000.0);
    bidder2.addBalance(10_000_000.0);

    processor = new AuctionFinancialProcessor();
    auction = new Auction("A001",
        new Electronics("e1", "TV", 500_000.0), 9999L, "seller");
    auction.setStatus(AuctionStatus.RUNNING);
  }

  // ===== Seller self-bid =====

  @Test
  void sellerCannotBidOwnAuction() {
    seller.addBalance(10_000_000.0);
    assertThrows(InvalidBidException.class,
        () -> processor.processTransaction(auction, seller, 600_000.0, null,
            (p, t) -> {
            }));
  }

  // ===== NaN / Infinite =====

  @Test
  void nanAmountThrows() {
    assertThrows(InvalidBidException.class,
        () -> processor.processTransaction(auction, bidder1, Double.NaN, null,
            (p, t) -> {
            }));
  }

  @Test
  void infiniteAmountThrows() {
    assertThrows(InvalidBidException.class,
        () -> processor.processTransaction(auction, bidder1, Double.POSITIVE_INFINITY, null,
            (p, t) -> {
            }));
  }

  // ===== Insufficient balance =====

  @Test
  void insufficientBalanceThrows() throws Exception {
    // Drain bidder1's balance
    bidder1.deductBalance(bidder1.getBalance());
    assertThrows(InvalidBidException.class,
        () -> processor.processTransaction(auction, bidder1, 600_000.0, null,
            (p, t) -> {
            }));
  }

  // ===== First bid (no previous bidder) =====

  @Test
  void firstBidDeductsFromBidder() throws Exception {
    double before = bidder1.getBalance();
    final double[] capturedPrice = { 0 };

    processor.processTransaction(auction, bidder1, 600_000.0, null,
        (price, tx) -> capturedPrice[0] = price);

    assertEquals(600_000.0, capturedPrice[0], 0.001);
    assertEquals(before - 600_000.0, bidder1.getBalance(), 0.001);
  }

  @Test
  void firstBidStateUpdaterReceivesCorrectTransaction() throws Exception {
    final List<BidTransaction> captured = new ArrayList<>();

    processor.processTransaction(auction, bidder1, 600_000.0, null,
        (price, tx) -> {
          captured.add(tx);
          // Simulate auction recording the transaction
          auction.getBidHistory().add(tx);
        });

    assertEquals(1, captured.size());
    assertEquals(bidder1, captured.get(0).getBidder());
    assertEquals(600_000.0, captured.get(0).getAmount(), 0.001);
  }

  // ===== Second bid (with previous bidder) =====

  @Test
  void secondBidRefundsPreviousBidder() throws Exception {
    // Bid 1: bidder1
    BidTransaction firstTx = new BidTransaction(bidder1, 600_000.0);
    bidder1.deductBalance(600_000.0);
    auction.getBidHistory().add(firstTx);

    double bidder1BalanceAfterFirstBid = bidder1.getBalance();

    // Bid 2: bidder2 outbids
    processor.processTransaction(auction, bidder2, 700_000.0, bidder1,
        (price, tx) -> auction.getBidHistory().add(tx));

    // bidder1 must be refunded 600_000
    assertEquals(bidder1BalanceAfterFirstBid + 600_000.0, bidder1.getBalance(), 0.001);
  }

  @Test
  void secondBidDeductsFromNewBidder() throws Exception {
    BidTransaction firstTx = new BidTransaction(bidder1, 600_000.0);
    bidder1.deductBalance(600_000.0);
    auction.getBidHistory().add(firstTx);

    double bidder2Before = bidder2.getBalance();

    processor.processTransaction(auction, bidder2, 700_000.0, bidder1,
        (price, tx) -> auction.getBidHistory().add(tx));

    assertEquals(bidder2Before - 700_000.0, bidder2.getBalance(), 0.001);
  }

  // ===== Same bidder outbids themselves =====

  @Test
  void sameBidderRaisingOwnBidNoRefund() throws Exception {
    BidTransaction firstTx = new BidTransaction(bidder1, 600_000.0);
    bidder1.deductBalance(600_000.0);
    auction.getBidHistory().add(firstTx);

    double before = bidder1.getBalance();

    // bidder1 bids again (previousBidder == bidder)
    processor.processTransaction(auction, bidder1, 700_000.0, bidder1,
        (price, tx) -> auction.getBidHistory().add(tx));

    // Should NOT be refunded (same bidder)
    assertEquals(before - 700_000.0, bidder1.getBalance(), 0.001);
  }
}