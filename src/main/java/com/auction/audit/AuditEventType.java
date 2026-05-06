package com.auction.audit;

/**
 * Định nghĩa tất cả các loại event có thể xảy ra trong hệ thống.
 * Sử dụng constants để tránh typo và dễ maintain.
 */
public final class AuditEventType {
    private AuditEventType() {} // Utility class - không cho khởi tạo
    
    // ===== AUTHENTICATION =====
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String LOGOUT = "LOGOUT";
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String REGISTER_FAILURE = "REGISTER_FAILURE";
    
    // ===== AUCTION LIFECYCLE =====
    public static final String AUCTION_CREATED = "AUCTION_CREATED";
    public static final String AUCTION_STARTED = "AUCTION_STARTED";
    public static final String AUCTION_CLOSED = "AUCTION_CLOSED";
    public static final String AUCTION_CANCELED = "AUCTION_CANCELED";
    
    // ===== BIDDING =====
    public static final String BID_PLACED = "BID_PLACED";
    public static final String BID_FAILED = "BID_FAILED";
    public static final String BID_RETRACTED = "BID_RETRACTED";
    public static final String BID_OUTBID = "BID_OUTBID"; // Khi bị người khác outbid
    
    // ===== AUTO-BIDDING =====
    public static final String AUTOBID_CONFIGURED = "AUTOBID_CONFIGURED";
    public static final String AUTOBID_EXECUTED = "AUTOBID_EXECUTED";
    public static final String AUTOBID_STOPPED = "AUTOBID_STOPPED";
    
    // ===== BALANCE OPERATIONS =====
    public static final String BALANCE_DEPOSIT = "BALANCE_DEPOSIT";
    public static final String BALANCE_WITHDRAW = "BALANCE_WITHDRAW";
    public static final String BALANCE_REFUND = "BALANCE_REFUND";
    public static final String BALANCE_DEDUCT = "BALANCE_DEDUCT";
    
    // ===== ADMIN ACTIONS =====
    public static final String ADMIN_FORCE_CLOSE_AUCTION = "ADMIN_FORCE_CLOSE_AUCTION";
    public static final String ADMIN_USER_BANNED = "ADMIN_USER_BANNED";
    public static final String ADMIN_USER_UNBANNED = "ADMIN_USER_UNBANNED";
    public static final String ADMIN_DATA_EXPORT = "ADMIN_DATA_EXPORT";
    
    // ===== SYSTEM EVENTS =====
    public static final String SYSTEM_STARTUP = "SYSTEM_STARTUP";
    public static final String SYSTEM_SHUTDOWN = "SYSTEM_SHUTDOWN";
    public static final String DATA_SAVED = "DATA_SAVED";
    public static final String DATA_LOADED = "DATA_LOADED";
    public static final String DATA_BACKUP = "DATA_BACKUP";
    
    // ===== WATCHLIST =====
    public static final String WATCHLIST_ADDED = "WATCHLIST_ADDED";
    public static final String WATCHLIST_REMOVED = "WATCHLIST_REMOVED";
    
    // ===== ANTI-SNIPING =====
    public static final String ANTI_SNIPING_TRIGGERED = "ANTI_SNIPING_TRIGGERED";
}