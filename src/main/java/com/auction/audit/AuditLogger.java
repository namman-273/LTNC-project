package com.auction.audit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Singleton audit logger với async writing.
 * Thread-safe và high-performance.
 * 
 * Usage:
 *   AuditLogger.getInstance().log(
 *       new AuditEvent.Builder()
 *           .eventType(AuditEventType.LOGIN_SUCCESS)
 *           .username("alice")
 *           .action("User logged in")
 *           .build()
 *   );
 */
public class AuditLogger {
    private static volatile AuditLogger instance;
    
    private final ExecutorService executor;
    private final Path auditDirectory;
    private static final String AUDIT_DIR = "audit-logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private AuditLogger() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AuditLogger-Writer");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
        
        this.auditDirectory = Paths.get(AUDIT_DIR);
        try {
            Files.createDirectories(auditDirectory);
            System.out.println("[AUDIT] Audit log directory created: " + 
                             auditDirectory.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[AUDIT] Failed to create audit directory: " + e.getMessage());
        }
    }
    
    public static AuditLogger getInstance() {
        if (instance == null) {
            synchronized (AuditLogger.class) {
                if (instance == null) {
                    instance = new AuditLogger();
                }
            }
        }
        return instance;
    }
    
    /**
     * Log an audit event asynchronously.
     * Non-blocking call — returns immediately.
     */
    public void log(AuditEvent event) {
        if (event == null) return;
        
        executor.submit(() -> {
            try {
                writeToFile(event);
                // Console output for development/debugging
                System.out.println("[AUDIT] " + event.toString());
            } catch (Exception e) {
                // NEVER throw from audit logger — would break application
                System.err.println("[AUDIT] Logging failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Convenience method: simple log
     */
    public void log(String eventType, String username, String action) {
        log(new AuditEvent.Builder()
            .eventType(eventType)
            .username(username)
            .action(action)
            .build());
    }
    
    /**
     * Write event to daily log file.
     * Format: audit-logs/audit-2025-05-06.log
     */
    private void writeToFile(AuditEvent event) throws IOException {
        String date = LocalDate.now().format(DATE_FORMATTER);
        Path logFile = auditDirectory.resolve("audit-" + date + ".log");
        
        String logLine = event.toJson() + System.lineSeparator();
        
        // Append to file (create if not exists)
        Files.write(logFile, logLine.getBytes(), 
                   StandardOpenOption.CREATE, 
                   StandardOpenOption.APPEND);
    }
    
    /**
     * Graceful shutdown — wait for pending logs to flush.
     * Call this before application exit.
     */
    public void shutdown() {
        System.out.println("[AUDIT] Shutting down audit logger...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("[AUDIT] Force shutdown (timeout)");
                executor.shutdownNow();
            } else {
                System.out.println("[AUDIT] All audit logs flushed successfully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get log file path for a specific date
     */
    public Path getLogFile(LocalDate date) {
        String filename = "audit-" + date.format(DATE_FORMATTER) + ".log";
        return auditDirectory.resolve(filename);
    }
    
    /**
     * Check if log file exists for a date
     */
    public boolean hasLogForDate(LocalDate date) {
        return Files.exists(getLogFile(date));
    }
}
