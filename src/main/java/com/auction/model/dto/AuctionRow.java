package com.auction.model.dto;

import com.auction.model.entities.Auction;

public class AuctionRow {
  private final String id;
  private final String itemName;
  private final double currentPrice;
  private final String status;
  private final long   endTime;
  private final String sellerId;
  private final String description;
  private final String imageUrl;
  // ── MỚI ──────────────────────────────────────────────────────────────────
  private final String itemType;       // "Art" | "Electronics" | "Vehicle"
  private final double startingPrice;  // Giá khởi điểm
  // ─────────────────────────────────────────────────────────────────────────

  /** Constructor từ Auction entity (phía BE dùng). */
  public AuctionRow(Auction a) {
    this.id           = a.getId();
    this.endTime      = a.getEndTime();
    this.itemName     = a.getItem().getItemName();
    this.currentPrice = a.getCurrentPrice();
    this.status       = a.getStatus().toString();
    String rawSellerId = a.getSellerId();
    this.sellerId     = (rawSellerId != null && !rawSellerId.trim().isEmpty())
            ? rawSellerId : "Anonymous";
    this.description    = a.getItem().getDescription();
    this.imageUrl       = a.getItem().getImageUrl();
    // MỚI: lấy tên class ngắn làm itemType (Art / Electronics / Vehicle)
    this.itemType       = a.getItem().getClass().getSimpleName();
    this.startingPrice  = a.getItem().getStartingPrice();
  }

  /** Constructor đơn giản dùng trong Admin placeholder. */
  public AuctionRow(String id, String itemName, double currentPrice,
                    String status, long endTime) {
    this.id            = id;
    this.itemName      = itemName;
    this.currentPrice  = currentPrice;
    this.status        = status;
    this.endTime       = endTime;
    this.sellerId      = "";
    this.description   = "";
    this.imageUrl      = "";
    this.itemType      = "";
    this.startingPrice = 0;
  }

  // ── Getters ───────────────────────────────────────────────────────────────
  public String getId()                   { return id;            }
  public String getItemName()             { return itemName;      }
  public double getCurrentPrice()         { return currentPrice;  }
  public String getStatus()               { return status;        }
  public long   getEndTime()              { return endTime;       }
  public String getSellerId()             { return sellerId;      }
  public String getDescription()          { return description;   }
  public String getImageUrl()             { return imageUrl;      }
  public String getItemType()             { return itemType;      }   // MỚI
  public double getStartingPrice()        { return startingPrice; }   // MỚI

  public String getCurrentPriceFormatted() {
    return String.format("%,.0f VNĐ", currentPrice);
  }
  public String getStartingPriceFormatted() {                         // MỚI
    return String.format("%,.0f VNĐ", startingPrice);
  }
}