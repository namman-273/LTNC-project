package com.auction.exception;

/**
 * Exception thrown when an invalid bid is made.
 */
public class InvalidBidException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs an InvalidBidException with the specified detail message.
   *
   * @param message the detail message
   */
  public InvalidBidException(String message) {
    super(message);
  }
}
