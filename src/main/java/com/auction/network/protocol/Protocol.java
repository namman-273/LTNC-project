package com.auction.network.protocol;

/**
 * .
 */
public class Protocol {
  // --- COMMANDS (Lệnh từ Client gửi lên) ---
  public static final String CMD_REGISTER = "REGISTER";
  public static final String CMD_LOGIN = "LOGIN";
  public static final String CMD_LIST_AUCTIONS = "LIST_AUCTIONS";
  public static final String CMD_BID = "BID";
  public static final String CMD_CREATE_AUCTION = "CREATE_AUCTION";
  public static final String CMD_END_AUCTION = "END_AUCTION";
  public static final String CMD_GET_HISTORY = "GET_HISTORY";
  public static final String CMD_DEPOSIT = "DEPOSIT";
  public static final String CMD_GET_BALANCE = "GET_BALANCE";
  public static final String CMD_WATCH = "WATCH";
  public static final String CMD_UNWATCH = "UNWATCH";
  public static final String CMD_DELETE_AUCTION = "DELETE_AUCTION";
  public static final String CMD_GET_WATCHLIST = "GET_WATCHLIST";
  public static final String CMD_ADD_AUTO_BID = "ADD_AUTO_BID";
  public static final String CMD_GET_PROFILE = "GET_PROFILE";
  public static final String CMD_UPDATE_EMAIL = "UPDATE_EMAIL";
  public static final String CMD_UPDATE_PASSWORD = "UPDATE_PASSWORD";
  public static final String CMD_GET_BID_HISTORY = "GET_BID_HISTORY";

  // --- RESPONSES (Phản hồi từ Server về Client) ---
  public static final String RES_REGISTER_SUCCESS = "REGISTER_SUCCESS";
  public static final String RES_REGISTER_FAILED = "REGISTER_FAILED";
  public static final String RES_LOGIN_SUCCESS = "LOGIN_SUCCESS";
  public static final String RES_LOGIN_FAILED = "LOGIN_FAILED";
  public static final String RES_LIST_SUCCESS = "LIST_AUCTIONS_SUCCESS";
  public static final String RES_BID_SUCCESS = "BID_SUCCESS";
  public static final String RES_HISTORY = "HISTORY_RES";
  public static final String RES_DELETE_SUCCESS = "DELETE_SUCCESS";
  public static final String RES_DEPOSIT_SUCCESS = "DEPOSIT_SUCCESS";
  public static final String RES_BALANCE_INFO = "BALANCE_INFO";
  public static final String RES_END_SUCCESS = "END_SUCCESS";
  public static final String RES_SUCCESS = "SUCCESS";
  public static final String RES_ADMIN_END_SUCCESS = "ADMIN_END_SUCCESS";
  public static final String RES_WATCHLIST = "RES_WATCHLIST";
  public static final String RES_WATCH_SUCCESS = "WATCH_SUCCESS";
  public static final String RES_UNWATCH_SUCCESS = "UNWATCH_SUCCESS";
  public static final String RES_AUTO_BID_SUCCESS = "AUTO_BID_SUCCESS";
  public static final String RES_PROFILE_INFO = "PROFILE_INFO";
  public static final String RES_BID_HISTORY = "BID_HISTORY_RES";
  public static final String ERROR = "ERROR";

  // --- THÔNG BÁO BIẾN ĐỘNG (PUSH NOTIFICATIONS) ---
  public static final String NOTI_BALANCE_CHANGED = "BALANCE_CHANGED";

  // Thông báo chung cho mọi người: Giá đã tăng
  public static final String NOTI_BID_UPDATE = "BID_UPDATE";

  /**
   * NOTI_OUTBID: Thông báo riêng cho người bị vượt giá
   * Format: OUTBID|auctionId|newBidder|newAmount
   * NEW: Notification đặc biệt chỉ gửi cho người đang giữ giá cao nhất
   * khi họ bị vượt giá bởi người khác.
   */
  public static final String NOTI_OUTBID = "OUTBID";

  /**
   * NOTI_REFUND: Thông báo khi tiền được hoàn lại vào ví
   * Format: REFUND|auctionId|refundAmount|reason
   * NEW: Notification khi user nhận lại tiền do bị outbid hoặc auction bị hủy.
   * Reasons:
   * - "OUTBID": Bị người khác vượt giá
   * - "AUCTION_CANCELLED": Phiên đấu giá bị hủy
   * 
   */
  public static final String NOTI_REFUND = "REFUND";

  /**
   * NOTI_AUCTION_CANCELLED: Thông báo phiên đấu giá bị hủy bởi Admin
   * Format: AUCTION_CANCELLED|auctionId|reason
   * NEW: Gửi cho TẤT CẢ participants khi Admin đóng phiên sớm.
   */
  public static final String NOTI_AUCTION_CANCELLED = "AUCTION_CANCELLED";

  // FIX: Thêm thông báo gia hạn thời gian (Anti-Sniping)
  public static final String NOTI_SNIPING_UPDATE = "SNIPING_UPDATE";

  // tất cả Client đang mở App sẽ thấy món hàng đó tự động hiện ra
  // trên JTable mà không cần phải bấm nút Refresh.
  public static final String NOTI_NEW_AUCTION = "NEW_AUCTION";

  // Delimiter (Ký tự phân tách)
  public static final String SEPARATOR = "|";
}