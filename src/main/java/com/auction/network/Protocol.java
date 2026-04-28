package com.auction.network;

public class Protocol {
    // --- COMMANDS (Lệnh từ Client gửi lên) ---
    public static final String CMD_REGISTER = "REGISTER";
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_LIST_AUCTIONS = "LIST_AUCTIONS";
    public static final String CMD_BID = "BID";
    public static final String CMD_CREATE_AUCTION = "CREATE_AUCTION";
    public static final String CMD_END_AUCTION = "END_AUCTION";
    public static final String CMD_GET_HISTORY = "GET_HISTORY";

    // --- RESPONSES (Phản hồi từ Server về Client) ---
    public static final String RES_REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String RES_REGISTER_FAILED = "REGISTER_FAILED";
    public static final String RES_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String RES_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String RES_LIST_SUCCESS = "LIST_AUCTIONS_SUCCESS";
    public static final String RES_BID_SUCCESS = "BID_SUCCESS";
    public static final String RES_HISTORY = "HISTORY_RES";
    public static final String RES_END_SUCCESS = "END_SUCCESS";
    public static final String RES_SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    
    // Delimiter (Ký tự phân tách)
    public static final String SEPARATOR = " | ";
}