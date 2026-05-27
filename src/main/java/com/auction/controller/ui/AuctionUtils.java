package com.auction.controller.ui;

/**
 * Tiện ích tĩnh dùng chung cho các controller đấu giá.
 *
 */
public final class AuctionUtils {

  private AuctionUtils() {
    // Không cho phép khởi tạo — class toàn static
  }

  // ── Minimum bid increment (mirror AuctionValidator phía server) ───────────
  // Trước đây copy y hệt trong BidController VÀ AutoBidController.
  public static long getMinimumIncrement(double price) {
    if (price < 1_000_000)
      return 50_000;
    if (price < 5_000_000)
      return 100_000;
    if (price < 10_000_000)
      return 250_000;
    return 500_000;
  }

  // ── Extract winner name từ RES_END_SUCCESS detail string ─────────────────
  // Trước đây có 3 phiên bản hơi khác nhau trong AuctionList, Bid, AutoBid.
  // Phiên bản hợp nhất xử lý cả "Winner: X" lẫn "Winner:X".
  public static String extractWinner(String detail) {
    if (detail == null)
      return "N/A";

    // Thử "Winner: " trước (có space)
    int idx = detail.indexOf("Winner: ");
    if (idx >= 0) {
      String rest = detail.substring(idx + 8).trim();
      int sep = rest.indexOf("|");
      return sep > 0 ? rest.substring(0, sep).trim() : rest.trim();
    }
    // Fallback "Winner:" (không có space)
    idx = detail.indexOf("Winner:");
    if (idx >= 0) {
      String rest = detail.substring(idx + 7).trim();
      int sep = rest.indexOf("|");
      return sep > 0 ? rest.substring(0, sep).trim() : rest.trim();
    }
    return "N/A";
  }

  // ── Format giá từ chuỗi thô (có thể chứa dấu phẩy, "VND", "VNĐ") ────────
  public static String formatPrice(String raw) {
    if (raw == null)
      return "N/A";
    try {
      double val = Double.parseDouble(
          raw.replace(",", "").replace(" VND", "").replace(" VNĐ", "").trim());
      if (val > 999_000_000_000.0 || val < 0)
        return "N/A";
      return String.format("%,.0f VNĐ", val);
    } catch (NumberFormatException e) {
      return raw;
    }
  }

  /** Overload tiện lợi khi đã có giá trị double. */
  public static String formatPrice(double val) {
    return String.format("%,.0f VNĐ", val);
  }

  // ── Kiểm tra trạng thái terminal ─────────────────────────────────────────
  public static boolean isCancelledStatus(String status) {
    return "CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status);
  }

  public static boolean isFinishedStatus(String status) {
    return "FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
  }

  public static boolean isTerminalStatus(String status) {
    return isFinishedStatus(status) || isCancelledStatus(status);
  }
}
