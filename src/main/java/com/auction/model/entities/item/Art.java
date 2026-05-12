package com.auction.model.entities.item;

/**
 * Art class representing artwork items in the auction system.
 */
public class Art extends Item {

  private static final long serialVersionUID = 1L;

  public Art(String id, String name, double price) {
    super(id, name, price);
  }

  public String getitemType() {
    return "Art";
  }

  @Override
  public void displayInfo() {
    System.out.println("[com.auction.model.Art] " + itemName);
    System.out.println("Giá khởi điểm:" + getStartingPrice());
  }
}
