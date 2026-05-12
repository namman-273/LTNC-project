package com.auction.model.entities.item;

/**
 *  * .
 *  
 */
public class Vehicle extends Item {
  private static final long serialVersionUID = 1L;

  public Vehicle(String id, String name, double price) {
    super(id, name, price);
  }

  public String getitemType() {
    return "Vehicle";
  }

  @Override
  public void displayInfo() {
    System.out.println("[com.auction.model.Vehicle] " + itemName + " - Mẫu:");
    System.out.println("Giá khởi điểm:" + getStartingPrice());
  }
}
