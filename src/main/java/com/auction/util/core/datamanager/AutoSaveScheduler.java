package com.auction.util.core.datamanager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quản lý auto-save scheduling.
 * Tuân thủ Single Responsibility Principle - chỉ làm việc scheduling.
 */
public class AutoSaveScheduler {
  
  private final long intervalMs;
  private final ScheduledExecutorService scheduler;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  
  /**
   * Constructor.
   * 
   * @param intervalMs Interval giữa các lần auto-save (milliseconds)
   */
  public AutoSaveScheduler(long intervalMs) {
    this.intervalMs = intervalMs;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "AutoSaveScheduler");
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * Bắt đầu auto-save với task được cung cấp.
   * 
   * @param saveTask Task sẽ chạy định kỳ
   */
  public void start(Runnable saveTask) {
    if (isRunning.compareAndSet(false, true)) {
      scheduler.scheduleWithFixedDelay(() -> {
        try {
          saveTask.run();
        } catch (Exception e) {
          System.err.println("[AutoSaveScheduler] Error during auto-save: " + e.getMessage());
          e.printStackTrace();
        }
      }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
      
      System.out.println("[AutoSaveScheduler] Started with interval: " 
          + (intervalMs / 1000) + " seconds");
    }
  }

  /**
   * Dừng scheduler gracefully.
   * 
   * @param timeoutSeconds Thời gian chờ tối đa (giây)
   */
  public void shutdown(int timeoutSeconds) {
    if (isRunning.compareAndSet(true, false)) {
      System.out.println("[AutoSaveScheduler] Shutting down...");
      
      scheduler.shutdown();
      
      try {
        if (!scheduler.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
          System.out.println("[AutoSaveScheduler] Force shutdown after timeout");
        } else {
          System.out.println("[AutoSaveScheduler] Shutdown complete");
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
        System.err.println("[AutoSaveScheduler] Interrupted during shutdown");
      }
    }
  }

  /**
   * Kiểm tra scheduler có đang chạy không.
   */
  public boolean isRunning() {
    return isRunning.get();
  }

  /**
   * Lấy interval hiện tại.
   */
  public long getIntervalMs() {
    return intervalMs;
  }
}
