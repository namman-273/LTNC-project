package com.auction.exception;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
 
import org.junit.jupiter.api.Test;
 
public class ExceptionTest {
 
  // --- InvalidBidException ---
 
  @Test
  void invalidBidExceptionGetMessageReturnsCorrectMessage() {
    assertEquals("Giá quá thấp", new InvalidBidException("Giá quá thấp").getMessage());
  }
 
  @Test
  void invalidBidExceptionIsInstanceOfException() {
    assertInstanceOf(Exception.class, new InvalidBidException("msg"));
  }
 
  @Test
  void invalidBidExceptionIsNotNull() {
    assertNotNull(new InvalidBidException("msg"));
  }
 
  // --- AuctionClosedException ---
 
  @Test
  void auctionClosedExceptionGetMessageReturnsCorrectMessage() {
    assertEquals("Phiên đã đóng", new AuctionClosedException("Phiên đã đóng").getMessage());
  }
 
  @Test
  void auctionClosedExceptionIsInstanceOfException() {
    assertInstanceOf(Exception.class, new AuctionClosedException("msg"));
  }
 
  @Test
  void auctionClosedExceptionIsNotNull() {
    assertNotNull(new AuctionClosedException("msg"));
  }
 
  // --- AuthenticationException ---
 
  @Test
  void authenticationExceptionGetMessageReturnsCorrectMessage() {
    assertEquals("Chưa đăng nhập", new AuthenticationException("Chưa đăng nhập").getMessage());
  }
 
  @Test
  void authenticationExceptionIsInstanceOfException() {
    assertInstanceOf(Exception.class, new AuthenticationException("msg"));
  }
 
  @Test
  void authenticationExceptionIsNotNull() {
    assertNotNull(new AuthenticationException("msg"));
  }
 
  // --- Có thể catch được ---
 
  @Test
  void invalidBidExceptionCanBeCaught() {
    try {
      throw new InvalidBidException("test");
    } catch (InvalidBidException e) {
      assertEquals("test", e.getMessage());
    }
  }
 
  @Test
  void auctionClosedExceptionCanBeCaught() {
    try {
      throw new AuctionClosedException("test");
    } catch (AuctionClosedException e) {
      assertEquals("test", e.getMessage());
    }
  }
 
  @Test
  void authenticationExceptionCanBeCaught() {
    try {
      throw new AuthenticationException("test");
    } catch (AuthenticationException e) {
      assertEquals("test", e.getMessage());
    }
  }
}