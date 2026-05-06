package com.auction.model;

/**
 * .
 */
public class Electronics extends Item {

  private static final long serialVersionUID = 1L;

  public Electronics(String id, String name, double price) {
    super(id, name, price);

  }

  @Override
  public void displayInfo() {
    System.out.println("[com.auction.model.Electronics] " + itemName);
    System.out.println("Giá khởi điểm:" + getStartingPrice());
  }
}
