package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.entities.user.Bidder;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.auctionhelpers.AuctionValidator;
import com.auction.util.exception.AuctionClosedException;
import com.auction.util.exception.AuthenticationException;
import com.auction.util.exception.InvalidBidException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bao phủ toàn bộ nhánh của AuctionValidator:
 * - getMinimumIncrement: 4 tier
 * - validateAuctionStatus: OPEN/hết giờ/FINISHED/PAID/CANCELED
 * - validateBidAmount: hợp lệ / quá thấp
 * - validateAuthentication: null / hợp lệ
 */
public class AuctionValidatorTest {

  private AuctionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new AuctionValidator();
  }

  // ===== getMinimumIncrement =====

  @Test
  void incrementTier1BelowOneMillion() {
    assertEquals(50_000.0, validator.getMinimumIncrement(999_999.0), 0.001);
  }

  @Test
  void incrementTier1AtZero() {
    assertEquals(50_000.0, validator.getMinimumIncrement(0.0), 0.001);
  }

  @Test
  void incrementTier2AtOneMillion() {
    assertEquals(100_000.0, validator.getMinimumIncrement(1_000_000.0), 0.001);
  }

  @Test
  void incrementTier2Below5Million() {
    assertEquals(100_000.0, validator.getMinimumIncrement(4_999_999.0), 0.001);
  }

  @Test
  void incrementTier3At5Million() {
    assertEquals(250_000.0, validator.getMinimumIncrement(5_000_000.0), 0.001);
  }

  @Test
  void incrementTier3Below10Million() {
    assertEquals(250_000.0, validator.getMinimumIncrement(9_999_999.0), 0.001);
  }

  @Test
  void incrementTier4At10Million() {
    assertEquals(500_000.0, validator.getMinimumIncrement(10_000_000.0), 0.001);
  }

  @Test
  void incrementTier4HighPrice() {
    assertEquals(500_000.0, validator.getMinimumIncrement(100_000_000.0), 0.001);
  }

  // ===== validateAuctionStatus =====

  @Test
  void openStatusWithFutureEndTimeDoesNotThrow() {
    long future = System.currentTimeMillis() + 60_000L;
    assertDoesNotThrow(
        () -> validator.validateAuctionStatus(AuctionStatus.OPEN, future));
  }

  @Test
  void runningStatusWithFutureEndTimeDoesNotThrow() {
    long future = System.currentTimeMillis() + 60_000L;
    assertDoesNotThrow(
        () -> validator.validateAuctionStatus(AuctionStatus.RUNNING, future));
  }

  @Test
  void expiredEndTimeThrowsRegardlessOfStatus() {
    long past = System.currentTimeMillis() - 1000L;
    assertThrows(AuctionClosedException.class,
        () -> validator.validateAuctionStatus(AuctionStatus.OPEN, past));
  }

  @Test
  void finishedStatusThrows() {
    long future = System.currentTimeMillis() + 60_000L;
    assertThrows(AuctionClosedException.class,
        () -> validator.validateAuctionStatus(AuctionStatus.FINISHED, future));
  }

  @Test
  void paidStatusThrows() {
    long future = System.currentTimeMillis() + 60_000L;
    assertThrows(AuctionClosedException.class,
        () -> validator.validateAuctionStatus(AuctionStatus.PAID, future));
  }

  @Test
  void canceledStatusThrows() {
    long future = System.currentTimeMillis() + 60_000L;
    assertThrows(AuctionClosedException.class,
        () -> validator.validateAuctionStatus(AuctionStatus.CANCELED, future));
  }

  // ===== validateBidAmount =====

  @Test
  void bidAmountExactlyAtMinRequiredDoesNotThrow() {
    // currentPrice = 500_000 → inc = 50_000 → min = 550_000
    assertDoesNotThrow(
        () -> validator.validateBidAmount(500_000.0, 550_000.0));
  }

  @Test
  void bidAmountAboveMinRequiredDoesNotThrow() {
    assertDoesNotThrow(
        () -> validator.validateBidAmount(500_000.0, 1_000_000.0));
  }

  @Test
  void bidAmountBelowMinRequiredThrows() {
    // currentPrice = 500_000 → inc = 50_000 → min = 550_000; bid at 549_999
    assertThrows(InvalidBidException.class,
        () -> validator.validateBidAmount(500_000.0, 549_999.0));
  }

  @Test
  void bidAmountEqualCurrentPriceThrows() {
    assertThrows(InvalidBidException.class,
        () -> validator.validateBidAmount(500_000.0, 500_000.0));
  }

  @Test
  void bidAmountZeroThrows() {
    assertThrows(InvalidBidException.class,
        () -> validator.validateBidAmount(500_000.0, 0.0));
  }

  @Test
  void bidAmountNegativeThrows() {
    assertThrows(InvalidBidException.class,
        () -> validator.validateBidAmount(500_000.0, -1.0));
  }

  // ===== validateAuthentication =====

  @Test
  void nullBidderThrowsAuthenticationException() {
    assertThrows(AuthenticationException.class,
        () -> validator.validateAuthentication(null));
  }

  @Test
  void validBidderDoesNotThrow() {
    Bidder b = new Bidder("user1", "hash", null);
    assertDoesNotThrow(() -> validator.validateAuthentication(b));
  }
}