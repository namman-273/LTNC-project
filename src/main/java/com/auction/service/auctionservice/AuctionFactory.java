package com.auction.service.auctionservice;

import com.auction.model.entities.Auction;
import com.auction.model.entities.item.Item;
import com.auction.model.factory.ItemFactory;
import com.auction.model.factory.ItemFactoryRegistry;

/**
 * Factory class để tạo Auction objects.
 * Tuân thủ Open/Closed Principle và Single Responsibility Principle.
 */
public class AuctionFactory {

  /**
   * Tạo auction mới với tất cả thông tin cần thiết.
   */
  public Auction createAuction(String itemType, String itemName, double startingPrice,
      long durationMinutes, String sellerId, String description, String imageUrl) {

    // 1. Tạo ID duy nhất cho phiên đấu giá
    String auctionId = generateAuctionId();

    // 2. Sử dụng ItemFactory để tạo Item
    Item newItem = createItem(itemType, auctionId, itemName, startingPrice,
        description, imageUrl);

    // 3. Khởi tạo đối tượng Auction mới
    return new Auction(auctionId, newItem, durationMinutes, sellerId);
  }

  /**
   * Tạo ID duy nhất cho auction.
   */
  private String generateAuctionId() {
    return "AUC_" + System.currentTimeMillis();
  }

  /**
   * Tạo Item object sử dụng ItemFactory pattern.
   */
  private Item createItem(String itemType, String auctionId, String itemName,
      double startingPrice, String description, String imageUrl) {

    ItemFactory factory = ItemFactoryRegistry.getFactory(itemType);
    Item item = factory.create(auctionId, itemName, startingPrice);
    item.setDescription(description);
    item.setImageUrl(imageUrl);

    return item;
  }
}
