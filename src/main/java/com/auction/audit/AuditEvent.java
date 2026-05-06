package com.auction.audit;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit event.
 * Once created, cannot be modified (tamper-proof).
 */
public final class AuditEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private final String id;              // Unique event ID
    private final Instant timestamp;      // When it happened
    private final String eventType;       // LOGIN, BID, CREATE_AUCTION, etc.
    private final String username;        // Who did it
    private final String ipAddress;       // From where
    private final String action;          // Human-readable action
    private final String resourceId;      // What was affected (auctionId, userId)
    private final String result;          // SUCCESS, FAILURE, ERROR
    private final Map<String, String> metadata; // Additional context
    
    private AuditEvent(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.eventType = builder.eventType;
        this.username = builder.username;
        this.ipAddress = builder.ipAddress;
        this.action = builder.action;
        this.resourceId = builder.resourceId;
        this.result = builder.result;
        this.metadata = new HashMap<>(builder.metadata); // Defensive copy
    }
    
    // Getters only (no setters — immutable!)
    public String getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getUsername() { return username; }
    public String getIpAddress() { return ipAddress; }
    public String getAction() { return action; }
    public String getResourceId() { return resourceId; }
    public String getResult() { return result; }
    public Map<String, String> getMetadata() { 
        return new HashMap<>(metadata); // Return copy
    }
    
    /**
     * Convert to JSON for storage
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":\"").append(escapeJson(id)).append("\",");
        json.append("\"timestamp\":\"").append(timestamp.toString()).append("\",");
        json.append("\"eventType\":\"").append(escapeJson(eventType)).append("\",");
        json.append("\"username\":\"").append(escapeJson(username)).append("\",");
        json.append("\"ipAddress\":\"").append(escapeJson(ipAddress)).append("\",");
        json.append("\"action\":\"").append(escapeJson(action)).append("\",");
        json.append("\"resourceId\":\"").append(escapeJson(resourceId)).append("\",");
        json.append("\"result\":\"").append(escapeJson(result)).append("\"");
        
        if (!metadata.isEmpty()) {
            json.append(",\"metadata\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
                first = false;
            }
            json.append("}");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Convert to CSV for Excel import
     */
    public String toCsv() {
        return String.join(",",
            id,
            FORMATTER.format(timestamp),
            eventType,
            username,
            ipAddress,
            "\"" + action.replace("\"", "\"\"") + "\"", // Escape quotes
            resourceId,
            result
        );
    }
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s by %s from %s (Resource: %s, Result: %s)",
            FORMATTER.format(timestamp), eventType, action, username, ipAddress, resourceId, result);
    }
    
    // ===== BUILDER PATTERN =====
    public static class Builder {
        private String eventType;
        private String username;
        private String ipAddress = "unknown";
        private String action;
        private String resourceId = "N/A";
        private String result = "SUCCESS";
        private Map<String, String> metadata = new HashMap<>();
        
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }
        
        public Builder result(String result) {
            this.result = result;
            return this;
        }
        
        public Builder addMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public AuditEvent build() {
            if (eventType == null || username == null || action == null) {
                throw new IllegalStateException(
                    "eventType, username, and action are required");
            }
            return new AuditEvent(this);
        }
    }
}
