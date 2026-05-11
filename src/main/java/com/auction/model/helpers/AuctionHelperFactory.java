package com.auction.model.helpers;

public class AuctionHelperFactory {
  private static AuctionHelperFactory instance;

  private AuctionHelperFactory() {
  }

  public static AuctionHelperFactory getInstance() {
    if (instance == null) {
      instance = new AuctionHelperFactory();
    }
    return instance;
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
