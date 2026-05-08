package com.auction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerConnectionTest {

    private ServerSocket testServer;
    private int testPort;
    private ServerConnection conn;

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        testServer = null;
        testPort = 0;
        conn = null;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) {
            try { conn.disconnect(); } catch (Exception ignored) {}
        }
        if (testServer != null && !testServer.isClosed()) {
            try { testServer.close(); } catch (Exception ignored) {}
        }
        resetSingleton();
    }

    private void resetSingleton() throws Exception {
        Field f = ServerConnection.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    private void startTestServer() throws Exception {
        testServer = new ServerSocket(0);
        testServer.setReuseAddress(true);
        testPort = testServer.getLocalPort();
    }

    private void acceptOneClientEcho() {
        Thread t = new Thread(() -> {
            try {
                Socket client = testServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                String line = in.readLine();
                if (line != null) out.println("ECHO:" + line);
                client.close();
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    private void acceptOneClientSilent() {
        Thread t = new Thread(() -> {
            try { Socket c = testServer.accept(); c.close(); } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // --- Tests không cần ServerSocket ---

    @Test
    void getInstanceReturnsSameObject() {
        ServerConnection a = ServerConnection.getInstance();
        ServerConnection b = ServerConnection.getInstance();
        assertTrue(a == b);
    }

    @Test
    void isConnectedReturnsFalseWhenNotConnected() {
        conn = ServerConnection.getInstance();
        assertFalse(conn.isConnected());
    }

    @Test
    void sendAndReceiveWhenNotConnectedReturnsError() {
        conn = ServerConnection.getInstance();
        assertTrue(conn.sendAndReceive("HELLO").startsWith("ERROR|"));
    }

    @Test
    void receiveWhenNotConnectedReturnsNull() {
        conn = ServerConnection.getInstance();
        assertNull(conn.receive());
    }

    @Test
    void disconnectWhenNotConnectedDoesNotThrow() {
        conn = ServerConnection.getInstance();
        conn.disconnect();
        assertFalse(conn.isConnected());
    }

    @Test
    void connectReturnsFalseWhenNoServer() {
        conn = ServerConnection.getInstance();
        assertFalse(conn.connect()); // port 9999 không có server
    }

    // --- Tests có ServerSocket ---

    @Test
    void connectDirectSucceedsWithLocalServer() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        assertTrue(conn.connectDirect());
        assertTrue(conn.isConnected());
    }

    @Test
    void connectDirectReturnsFalseWhenNoServer() throws Exception {
        startTestServer();
        int port = testPort;
        testServer.close();
        testServer = null;
        conn = new ServerConnection("localhost", port);
        assertFalse(conn.connectDirect());
        assertFalse(conn.isConnected());
    }

    @Test
    void sendAndReceiveEchoesResponse() throws Exception {
        startTestServer();
        acceptOneClientEcho();
        conn = new ServerConnection("localhost", testPort);
        assertTrue(conn.connectDirect());
        assertEquals("ECHO:PING", conn.sendAndReceive("PING"));
    }

    @Test
    void disconnectWhenConnectedClosesSocket() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        conn.disconnect();
        assertFalse(conn.isConnected());
    }

    @Test
    void isConnectedTrueAfterSuccessfulConnect() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        assertTrue(conn.isConnected());
    }
}