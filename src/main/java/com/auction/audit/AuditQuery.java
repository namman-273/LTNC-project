package com.auction.audit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query và phân tích audit logs.
 * Hỗ trợ filter theo username, event type, date range, result.
 */
public class AuditQuery {
    private final AuditLogger logger = AuditLogger.getInstance();
    
    /**
     * Tìm tất cả events của một user trong một ngày
     */
    public List<String> findByUsername(String username, LocalDate date) throws IOException {
        Path logFile = logger.getLogFile(date);
        if (!Files.exists(logFile)) {
            return new ArrayList<>();
        }
        
        return Files.lines(logFile)
            .filter(line -> line.contains("\"username\":\"" + username + "\""))
            .collect(Collectors.toList());
    }
    
    /**
     * Tìm tất cả events theo loại event trong một ngày
     */
    public List<String> findByEventType(String eventType, LocalDate date) throws IOException {
        Path logFile = logger.getLogFile(date);
        if (!Files.exists(logFile)) {
            return new ArrayList<>();
        }
        
        return Files.lines(logFile)
            .filter(line -> line.contains("\"eventType\":\"" + eventType + "\""))
            .collect(Collectors.toList());
    }
    
    /**
     * Tìm tất cả events thất bại (FAILURE hoặc ERROR)
     */
    public List<String> findFailures(LocalDate date) throws IOException {
        Path logFile = logger.getLogFile(date);
        if (!Files.exists(logFile)) {
            return new ArrayList<>();
        }
        
        return Files.lines(logFile)
            .filter(line -> line.contains("\"result\":\"FAILURE\"") || 
                           line.contains("\"result\":\"ERROR\""))
            .collect(Collectors.toList());
    }
    
    /**
     * Tìm events liên quan đến một resource cụ thể (auctionId, etc.)
     */
    public List<String> findByResourceId(String resourceId, LocalDate date) throws IOException {
        Path logFile = logger.getLogFile(date);
        if (!Files.exists(logFile)) {
            return new ArrayList<>();
        }
        
        return Files.lines(logFile)
            .filter(line -> line.contains("\"resourceId\":\"" + resourceId + "\""))
            .collect(Collectors.toList());
    }
    
    /**
     * Tìm events trong một khoảng thời gian (date range)
     */
    public List<String> findByDateRange(LocalDate startDate, LocalDate endDate, 
                                        String eventType) throws IOException {
        List<String> results = new ArrayList<>();
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Path logFile = logger.getLogFile(current);
            if (Files.exists(logFile)) {
                List<String> dayEvents = eventType == null ? 
                    Files.readAllLines(logFile) :
                    findByEventType(eventType, current);
                results.addAll(dayEvents);
            }
            current = current.plusDays(1);
        }
        
        return results;
    }
    
    /**
     * Lấy thống kê tóm tắt cho một user trong một ngày
     */
    public Map<String, Long> getUserStatistics(String username, LocalDate date) 
            throws IOException {
        List<String> events = findByUsername(username, date);
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) events.size());
        stats.put("logins", countByType(events, AuditEventType.LOGIN_SUCCESS));
        stats.put("bids", countByType(events, AuditEventType.BID_PLACED));
        stats.put("failures", events.stream()
            .filter(e -> e.contains("\"result\":\"FAILURE\""))
            .count());
        stats.put("autobids", countByType(events, AuditEventType.AUTOBID_CONFIGURED));
        
        return stats;
    }
    
    /**
     * Lấy tổng quan hệ thống trong một ngày
     */
    public Map<String, Object> getSystemOverview(LocalDate date) throws IOException {
        Path logFile = logger.getLogFile(date);
        if (!Files.exists(logFile)) {
            return new HashMap<>();
        }
        
        List<String> allEvents = Files.readAllLines(logFile);
        
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalEvents", allEvents.size());
        overview.put("uniqueUsers", countUniqueUsers(allEvents));
        overview.put("loginAttempts", countByType(allEvents, AuditEventType.LOGIN_SUCCESS) + 
                                     countByType(allEvents, AuditEventType.LOGIN_FAILURE));
        overview.put("totalBids", countByType(allEvents, AuditEventType.BID_PLACED));
        overview.put("auctionsCreated", countByType(allEvents, AuditEventType.AUCTION_CREATED));
        overview.put("auctionsClosed", countByType(allEvents, AuditEventType.AUCTION_CLOSED));
        overview.put("failures", allEvents.stream()
            .filter(e -> e.contains("\"result\":\"FAILURE\""))
            .count());
        
        return overview;
    }
    
    /**
     * Export logs sang CSV format
     */
    public String exportToCsv(LocalDate date) throws IOException {
        Path logFile = logger.getLogFile(date);
        if (!Files.exists(logFile)) {
            return "No logs for date: " + date;
        }
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Timestamp,EventType,Username,IPAddress,Action,ResourceId,Result\n");
        
        List<String> lines = Files.readAllLines(logFile);
        for (String line : lines) {
            // Parse JSON và convert sang CSV
            // Simplified version - in production use proper JSON parser
            String[] parts = line.split(",");
            if (parts.length > 0) {
                csv.append(line.replace("{", "").replace("}", "")
                          .replace("\"", "")).append("\n");
            }
        }
        
        return csv.toString();
    }
    
    // ===== HELPER METHODS =====
    
    private long countByType(List<String> events, String eventType) {
        return events.stream()
            .filter(e -> e.contains("\"eventType\":\"" + eventType + "\""))
            .count();
    }
    
    private long countUniqueUsers(List<String> events) {
        return events.stream()
            .map(e -> extractUsername(e))
            .filter(u -> u != null)
            .distinct()
            .count();
    }
    
    private String extractUsername(String jsonLine) {
        int start = jsonLine.indexOf("\"username\":\"");
        if (start == -1) return null;
        start += 12; // length of "username":"
        int end = jsonLine.indexOf("\"", start);
        if (end == -1) return null;
        return jsonLine.substring(start, end);
    }
}
