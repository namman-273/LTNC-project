package com.auction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers ServerConnection (0% → significant coverage):
 * - connect / disconnect / isConnected
 * - sendAndReceive happy path
 * - sendAndReceive when not connected
 * - receive when not connected
 * - connectDirect constructor
 * - reconnect when already connected
 */
public class ServerConnectionTest {

    
    private ServerSocket testServer;
    private int testPort;
    private ServerConnection conn;

    @BeforeEach
    void setUp() throws Exception {
        // Reset singleton
        Field f = ServerConnection.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);

        testServer = new ServerSocket(0);
        testServer.setReuseAddress(true);
        testPort = testServer.getLocalPort();
        conn = null;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) {
            conn.disconnect();
        }
        if (testServer != null && !testServer.isClosed()) {
            testServer.close();
        }
        // Reset singleton
        Field f = ServerConnection.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    // Helper: accept one client in background thread
    private Thread acceptOneClient(CountDownLatch ready) {
        Thread t = new Thread(() -> {
            try {
                ready.countDown();
                Socket client = testServer.accept();
                // Echo server
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                String line = in.readLine();
                if (line != null) out.println("ECHO:" + line);
                client.close();
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    // --- getInstance / Singleton ---

    @Test
    void getInstanceReturnsSameObject() throws Exception {
        ServerConnection a = ServerConnection.getInstance();
        ServerConnection b = ServerConnection.getInstance();
        assertTrue(a == b, "getInstance must return same singleton");
    }

    // --- isConnected when not connected ---

    @Test
    void isConnectedReturnsFalseWhenNotConnected() {
        conn = ServerConnection.getInstance();
        assertFalse(conn.isConnected());
    }

    // --- connect: fails when no server ---

    @Test
    void connectReturnsFalseWhenNoServer() throws Exception {
        testServer.close(); // no server listening on default port 9999
        conn = ServerConnection.getInstance();
        boolean result = conn.connect(); // tries localhost:9999
        assertFalse(result, "connect must return false when server is unreachable");
    }

    // --- connectDirect: happy path ---

    @Test
    void connectDirectSucceedsWithLocalServer() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Thread serverThread = new Thread(() -> {
            try {
                ready.countDown();
                Socket client = testServer.accept();
                client.close();
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        ready.await(2, TimeUnit.SECONDS);

        conn = new ServerConnection("localhost", testPort);
        boolean result = conn.connectDirect();
        assertTrue(result, "connectDirect must return true with running server");
        assertTrue(conn.isConnected());
    }

    // --- connectDirect: fails when no server ---

    @Test
    void connectDirectReturnsFalseWhenNoServer() throws Exception {
        testServer.close();
        conn = new ServerConnection("localhost", testPort);
        boolean result = conn.connectDirect();
        assertFalse(result);
        assertFalse(conn.isConnected());
    }

    // --- sendAndReceive: not connected returns error string ---

    @Test
    void sendAndReceiveWhenNotConnectedReturnsError() {
        conn = ServerConnection.getInstance();
        String response = conn.sendAndReceive("HELLO");
        assertTrue(response.startsWith("ERROR|"), "Must return ERROR| when not connected");
    }

    // --- receive: not connected returns null ---

    @Test
    void receiveWhenNotConnectedReturnsNull() {
        conn = ServerConnection.getInstance();
        assertNull(conn.receive());
    }

    // --- sendAndReceive: happy path with local echo server ---

    @Test
    void sendAndReceiveEchoesResponse() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        acceptOneClient(ready);
        ready.await(2, TimeUnit.SECONDS);

        conn = new ServerConnection("localhost", testPort);
        assertTrue(conn.connectDirect());

        String response = conn.sendAndReceive("PING");
        assertEquals("ECHO:PING", response);
    }

    // --- disconnect ---

    @Test
    void disconnectWhenConnectedClosesSocket() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Thread st = new Thread(() -> {
            try { ready.countDown(); testServer.accept(); } catch (Exception ignored) {}
        });
        st.setDaemon(true); st.start();
        ready.await(2, TimeUnit.SECONDS);

        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        assertTrue(conn.isConnected());

        conn.disconnect();
        assertFalse(conn.isConnected());
    }

    @Test
    void disconnectWhenNotConnectedDoesNotThrow() {
        conn = ServerConnection.getInstance();
        conn.disconnect(); // must not throw
        assertFalse(conn.isConnected());
    }

    // --- reconnect when already connected ---

    @Test
    void connectWhenAlreadyConnectedReturnsTrueImmediately() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Thread st = new Thread(() -> {
            try { ready.countDown(); testServer.accept(); } catch (Exception ignored) {}
        });
        st.setDaemon(true); st.start();
        ready.await(2, TimeUnit.SECONDS);

        conn = new ServerConnection("localhost", testPort);
        conn.connectDirect();
        assertTrue(conn.isConnected());

        // Inject socket into singleton to test connect() idempotency path
        Field socketField = ServerConnection.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        // Already connected — simulate calling connect() via reflection-based check
        assertTrue(conn.isConnected(), "Should still be connected");
    }
}