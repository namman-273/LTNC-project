package com.auction.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends Exception {
  /**
   * Constructs an AuthenticationException with the specified message.
   *
   * @param message the detail message
   */
  public AuthenticationException(String message) {
    super(message);
  }
}
//