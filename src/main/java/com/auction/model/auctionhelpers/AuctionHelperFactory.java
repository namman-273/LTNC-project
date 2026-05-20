package com.auction.model.auctionhelpers;

/**
 * Simple Factory.
 */
public class AuctionHelperFactory {

  private AuctionHelperFactory() {
  }

  // Tạo một lớp static lồng bên trong (Holder),cơ chế Bill Pugh Singleton
  private static class Holder {
    private static final AuctionHelperFactory INSTANCE = new AuctionHelperFactory();
  }

  /**
   * singelton.
   */
  public static AuctionHelperFactory getInstance() {
    return Holder.INSTANCE;
  }

  public AuctionValidator createValidator() {
    return new AuctionValidator();
  }

  public AuctionNotifier createNotifier() {
    return new AuctionNotifier();
  }

  public AutoBidProcessor createAutoBidProcessor() {
    return new AutoBidProcessor();
  }

  public AuctionFinancialProcessor createFinancialProcessor() {
    return new AuctionFinancialProcessor();
  }

  public AuctionSnipingProcessor createSnipingProcessor() {
    return new AuctionSnipingProcessor();
  }
}
