package com.auction.util.core.datamanager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quản lý dirty flags cho từng loại data.
 * Tuân thủ Single Responsibility Principle - chỉ làm 1 việc: track dirty state.
 */
public class DirtyFlagTracker {
  private final AtomicBoolean usersDirty = new AtomicBoolean(false);
  private final AtomicBoolean auctionsDirty = new AtomicBoolean(false);
  private final AtomicBoolean historyDirty = new AtomicBoolean(false);

  /**
   * Đánh dấu users dirty.
   */
  public void markUsersDirty() {
    usersDirty.set(true);
  }

  /**
   * Đánh dấu auctions dirty.
   */
  public void markAuctionsDirty() {
    auctionsDirty.set(true);
  }

  /**
   * Đánh dấu history dirty.
   */
  public void markHistoryDirty() {
    historyDirty.set(true);
  }

  /**
   * Kiểm tra users có dirty không.
   */
  public boolean isUsersDirty() {
    return usersDirty.get();
  }

  /**
   * Kiểm tra auctions có dirty không.
   */
  public boolean isAuctionsDirty() {
    return auctionsDirty.get();
  }

  /**
   * Kiểm tra history có dirty không.
   */
  public boolean isHistoryDirty() {
    return historyDirty.get();
  }

  /**
   * Clear users dirty flag (trả về giá trị cũ).
   */
  public boolean clearUsersDirty() {
    return usersDirty.compareAndSet(true, false);
  }

  /**
   * Clear auctions dirty flag (trả về giá trị cũ).
   */
  public boolean clearAuctionsDirty() {
    return auctionsDirty.compareAndSet(true, false);
  }

  /**
   * Clear history dirty flag (trả về giá trị cũ).
   */
  public boolean clearHistoryDirty() {
    return historyDirty.compareAndSet(true, false);
  }

  /**
   * Force set tất cả dirty flags (dùng cho shutdown).
   */
  public void markAllDirty() {
    usersDirty.set(true);
    auctionsDirty.set(true);
    historyDirty.set(true);
  }

  /**
   * Kiểm tra có ít nhất 1 dirty flag không.
   */
  public boolean hasAnyDirty() {
    return usersDirty.get() || auctionsDirty.get() || historyDirty.get();
  }

}
