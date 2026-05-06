package com.auction.exception;

/**
 * Exception thrown when an auction is closed.
 */
public class AuctionClosedException extends Exception {
  /**
   * Constructs an AuctionClosedException with the specified message.
   *
   * @param message the detail message
   */
  public AuctionClosedException(String message) {
    super(message);
  }
}
