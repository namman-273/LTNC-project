package com.auction.util;

import static org.junit.jupiter.api.Assertions.*;

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

    /** Khởi động mock server trên port ngẫu nhiên */
    private void startTestServer() throws Exception {
        testServer = new ServerSocket(0);
        testServer.setReuseAddress(true);
        testPort = testServer.getLocalPort();
    }

    /** Mock server: accept 1 client rồi echo lại "ECHO:<message>" */
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

    /** Mock server: accept rồi đóng ngay (không gửi gì) */
    private void acceptOneClientSilent() {
        Thread t = new Thread(() -> {
            try { Socket c = testServer.accept(); c.close(); } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Singleton ─────────────────────────────────────────────

    @Test
    void getInstanceReturnsSameObject() {
        ServerConnection a = ServerConnection.getInstance();
        ServerConnection b = ServerConnection.getInstance();
        assertSame(a, b);
    }

    // ── isConnected trước khi kết nối ─────────────────────────

    @Test
    void isConnectedReturnsFalseWhenNotConnected() {
        conn = ServerConnection.getInstance();
        assertFalse(conn.isConnected());
    }

    // ── connect() fail khi không có server ────────────────────

    @Test
    void connectReturnsFalseWhenNoServer() {
        conn = ServerConnection.getInstance(); // host=localhost, port=9999 — không có ai
        assertFalse(conn.connect());
    }

    // ── sendAndReceive khi offline ────────────────────────────

    @Test
    void sendAndReceiveWhenNotConnectedReturnsError() {
        conn = ServerConnection.getInstance();
        String resp = conn.sendAndReceive("HELLO");
        assertTrue(resp.startsWith("ERROR|"),
            "Expected ERROR| prefix, got: " + resp);
    }

    // ── receive() khi offline ─────────────────────────────────

    @Test
    void receiveWhenNotConnectedReturnsNull() {
        conn = ServerConnection.getInstance();
        assertNull(conn.receive());
    }

    // ── disconnect khi chưa kết nối ───────────────────────────

    @Test
    void disconnectWhenNotConnectedDoesNotThrow() {
        conn = ServerConnection.getInstance();
        assertDoesNotThrow(() -> conn.disconnect());
    }

    @Test
    void isConnectedFalseAfterDisconnectWithoutConnect() {
        conn = ServerConnection.getInstance();
        conn.disconnect();
        assertFalse(conn.isConnected());
    }

    // ── connectDirect() với mock server ──────────────────────

    @Test
    void connectDirectSucceedsWithLocalServer() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        assertTrue(conn.connectDirect());
        assertTrue(conn.isConnected());
    }

    @Test
    void connectDirectReturnsFalseWhenServerClosed() throws Exception {
        startTestServer();
        int port = testPort;
        testServer.close();
        testServer = null;
        conn = new ServerConnection("localhost", port);
        assertFalse(conn.connectDirect());
        assertFalse(conn.isConnected());
    }

    // ── sendAndReceive với mock server ───────────────────────

    @Test
    void sendAndReceiveEchoesResponse() throws Exception {
        startTestServer();
        acceptOneClientEcho();
        conn = new ServerConnection("localhost", testPort);
        assertTrue(conn.connectDirect());
        String resp = conn.sendAndReceive("PING");
        assertEquals("ECHO:PING", resp);
    }

    // ── disconnect khi đang kết nối ───────────────────────────

    @Test
    void disconnectWhenConnectedClosesSocket() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        conn.disconnect();
        assertFalse(conn.isConnected());
    }

    // ── isConnected sau khi kết nối thành công ────────────────

    @Test
    void isConnectedTrueAfterSuccessfulConnectDirect() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        assertTrue(conn.isConnected());
    }

    // ── disconnectDirect ─────────────────────────────────────

    @Test
    void disconnectDirectClosesSocket() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        assertDoesNotThrow(() -> conn.disconnectDirect());
        assertFalse(conn.isConnected());
    }

    @Test
    void disconnectDirectWhenNotConnectedDoesNotThrow() {
        conn = new ServerConnection("localhost", 19999);
        assertDoesNotThrow(() -> conn.disconnectDirect());
    }

    // ── connect() idempotent khi đã connected ─────────────────

    @Test
    void connectWhenAlreadyConnectedReturnsTrue() throws Exception {
        startTestServer();
        acceptOneClientSilent();
        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        // Giờ gọi connect() — socket đã connected, phải trả true ngay
        assertTrue(conn.connect());
    }
}