package com.auction.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Singleton quản lý thông báo toàn app.
 */
public class NotificationManager {

    private static NotificationManager instance;
    private final ObservableList<String> notifications = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void add(String message) {
        String time = LocalDateTime.now().format(FORMATTER);
        notifications.add(0, time + "  •  " + message);
    }

    public ObservableList<String> getAll() {
        return notifications;
    }

    public void clear() {
        notifications.clear();
    }

    public int size() {
        return notifications.size();
    }
}