package com.auction.network;

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
  public static final String CMD_GET_WATCHLIST = "GET_WATCHLIST";
  public static final String CMD_ADD_AUTO_BID = "ADD_AUTO_BID";

  // --- RESPONSES (Phản hồi từ Server về Client) ---
  public static final String RES_REGISTER_SUCCESS = "REGISTER_SUCCESS";
  public static final String RES_REGISTER_FAILED = "REGISTER_FAILED";
  public static final String RES_LOGIN_SUCCESS = "LOGIN_SUCCESS";
  public static final String RES_LOGIN_FAILED = "LOGIN_FAILED";
  public static final String RES_LIST_SUCCESS = "LIST_AUCTIONS_SUCCESS";
  public static final String RES_BID_SUCCESS = "BID_SUCCESS";
  public static final String RES_HISTORY = "HISTORY_RES";
  public static final String RES_DEPOSIT_SUCCESS = "DEPOSIT_SUCCESS";
  public static final String RES_BALANCE_INFO = "BALANCE_INFO";
  public static final String RES_END_SUCCESS = "END_SUCCESS";
  public static final String RES_SUCCESS = "SUCCESS";
  public static final String RES_WATCHLIST = "RES_WATCHLIST";
  public static final String RES_WATCH_SUCCESS = "WATCH_SUCCESS";
  public static final String RES_UNWATCH_SUCCESS = "UNWATCH_SUCCESS";
  public static final String RES_AUTO_BID_SUCCESS = "AUTO_BID_SUCCESS";
  public static final String ERROR = "ERROR";

  // --- THÔNG BÁO BIẾN ĐỘNG (PUSH NOTIFICATIONS) ---
  // Dùng để báo cho Client biết tiền vừa bị trừ hoặc vừa được hoàn
  public static final String NOTI_BALANCE_CHANGED = "BALANCE_CHANGED";

  // Delimiter (Ký tự phân tách)
  public static final String SEPARATOR = "|";
}