package com.auction.exception;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
 
import org.junit.jupiter.api.Test;
 
public class ExceptionTest {
 
  @Test
  void invalidBidExceptionGetMessageReturnsCorrectMessage() {
    assertEquals("Giá quá thấp", new InvalidBidException("Giá quá thấp").getMessage());
  }
 
  @Test
  void auctionClosedExceptionGetMessageReturnsCorrectMessage() {
    assertEquals("Phiên đã đóng", new AuctionClosedException("Phiên đã đóng").getMessage());
  }
 
  @Test
  void authenticationExceptionGetMessageReturnsCorrectMessage() {
    assertEquals("Chưa đăng nhập", new AuthenticationException("Chưa đăng nhập").getMessage());
  }
 
  @Test
  void invalidBidExceptionIsInstanceOfException() {
    assertInstanceOf(Exception.class, new InvalidBidException("msg"));
  }
 
  @Test
  void auctionClosedExceptionIsInstanceOfException() {
    assertInstanceOf(Exception.class, new AuctionClosedException("msg"));
  }
 
  @Test
  void authenticationExceptionIsInstanceOfException() {
    assertInstanceOf(Exception.class, new AuthenticationException("msg"));
  }
}