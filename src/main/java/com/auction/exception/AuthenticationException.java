package com.auction.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends Exception {
  private static final long serialVersionUID = 1L;

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