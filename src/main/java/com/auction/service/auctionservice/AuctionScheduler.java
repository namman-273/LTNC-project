package com.auction.service.auctionservice;

import com.auction.model.entities.Auction;
import com.auction.model.enums.AuctionStatus;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service quản lý scheduling cho việc tự động đóng auctions.
 * Tuân thủ Single Responsibility Principle.
 */
public class AuctionScheduler {

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

  /**
   * Lên lịch đóng auction sau một khoảng thời gian.
   */
  public void scheduleAuctionEnd(String auctionId, long delayMinutes, Runnable endAuctionTask) {
    scheduler.schedule(endAuctionTask, delayMinutes, TimeUnit.MINUTES);
  }

  /**
   * Lên lịch đóng auction với delay tính bằng milliseconds.
   */
  public void scheduleAuctionEndWithDelay(String auctionId, long delayMillis,
      Runnable endAuctionTask) {
    scheduler.schedule(endAuctionTask, delayMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Khôi phục lịch trình cho các auction đang mở sau khi system restart.
   */
  public void recoverScheduledTasks(AuctionRepository repository, Runnable endAuctionTask) {
    if (scheduler == null || scheduler.isShutdown()) {
      scheduler = Executors.newScheduledThreadPool(5);
    }

    long now = System.currentTimeMillis();
    for (Auction auction : repository.getAllAuctions()) {
      if (auction.getStatus() == AuctionStatus.OPEN) {
        long delay = auction.getEndTime() - now;

        if (delay > 0) {
          // Nếu vẫn còn thời gian -> Lên lịch lại
          scheduler.schedule(endAuctionTask, delay, TimeUnit.MILLISECONDS);
        } else {
          // Nếu đã hết giờ -> Đóng luôn
          endAuctionTask.run();
        }
      }
    }
    System.out.println("[SCHEDULER] Đã khôi phục lịch trình đóng phiên cho các đấu giá đang mở.");
  }

  /**
   * Shutdown scheduler gracefully.
   */
  public void shutdown() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Kiểm tra scheduler đã shutdown chưa.
   */
  public boolean isShutdown() {
    return scheduler == null || scheduler.isShutdown();
  }

  /**
   * Khởi tạo lại scheduler nếu cần.
   */
  public void reinitialize() {
    if (scheduler == null || scheduler.isShutdown()) {
      scheduler = Executors.newScheduledThreadPool(5);
    }
  }
}