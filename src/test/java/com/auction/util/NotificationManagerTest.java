package com.auction.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NotificationManagerTest {

    @BeforeEach
    void reset() throws Exception {
        Field f = NotificationManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void singletonSameInstance() {
        assertSame(NotificationManager.getInstance(), NotificationManager.getInstance());
    }

    @Test
    void initiallyEmpty() {
        assertEquals(0, NotificationManager.getInstance().size());
    }

    @Test
    void addIncreasesSize() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("Test notification");
        assertEquals(1, nm.size());
    }

    @Test
    void addMultipleMessages() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("First");
        nm.add("Second");
        nm.add("Third");
        assertEquals(3, nm.size());
    }

    @Test
    void clearResetsSize() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("Something");
        nm.add("Another");
        nm.clear();
        assertEquals(0, nm.size());
    }

    @Test
    void getAllReturnsNotNull() {
        assertNotNull(NotificationManager.getInstance().getAll());
    }

    @Test
    void addedMessageContainsOriginalText() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("hello world");
        // message được prepend với timestamp, nhưng vẫn phải chứa text gốc
        assertTrue(nm.getAll().get(0).contains("hello world"));
    }

    @Test
    void latestMessageAddedFirst() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("first");
        nm.add("second");
        // add(0, ...) nên phần tử mới nhất ở index 0
        assertTrue(nm.getAll().get(0).contains("second"));
    }

    @Test
    void clearThenAddWorks() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("before clear");
        nm.clear();
        nm.add("after clear");
        assertEquals(1, nm.size());
        assertTrue(nm.getAll().get(0).contains("after clear"));
    }

    @Test
    void sizeMatchesGetAllSize() {
        NotificationManager nm = NotificationManager.getInstance();
        nm.add("a");
        nm.add("b");
        assertEquals(nm.size(), nm.getAll().size());
    }
}