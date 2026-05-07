package com.auction.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;
    private static final int MAX_RETRY = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private String host;
    private int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static volatile ServerConnection instance;

    private ServerConnection() {
        this.host = HOST;
        this.port = PORT;
    }

    public static ServerConnection getInstance() {
        if (instance == null) {
            synchronized (ServerConnection.class) {
                if (instance == null) {
                    instance = new ServerConnection();
                }
            }
        }
        return instance;
    }

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        synchronized (this) {
            try {
                if (socket != null && !socket.isClosed() && socket.isConnected()) {
                    return true;
                }
                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("Kết nối server thành công!");
                return true;
            } catch (Exception e) {
                System.err.println("Không thể kết nối server: " + e.getMessage());
                socket = null;
                return false;
            }
        }
    }

    /**
     * Kết nối với retry tự động — thử lại MAX_RETRY lần nếu thất bại.
     */
    public boolean connectWithRetry() {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            System.out.println("Đang kết nối server... (lần " + attempt + "/" + MAX_RETRY + ")");
            if (connect()) {
                return true;
            }
            if (attempt < MAX_RETRY) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        System.err.println("Không thể kết nối sau " + MAX_RETRY + " lần thử!");
        return false;
    }

    public boolean connectDirect() {
        try {
            socket = new Socket(this.host, this.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Kết nối trực tiếp thành công!");
            return true;
        } catch (Exception e) {
            System.err.println("Không thể kết nối: " + e.getMessage());
            return false;
        }
    }

    public synchronized String sendAndReceive(String message) {
        // Thử gửi, nếu mất kết nối thì retry 1 lần
        try {
            if (!isConnected()) {
                System.out.println("Mất kết nối, đang thử kết nối lại...");
                if (!connectWithRetry()) {
                    return "ERROR|Không thể kết nối server sau nhiều lần thử!";
                }
            }
            out.println(message);

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                if (!in.ready()) break;
            }
            return sb.toString();

        } catch (Exception e) {
            System.err.println("Lỗi gửi/nhận: " + e.getMessage());
            socket = null;

            // Retry 1 lần sau khi mất kết nối
            System.out.println("Đang thử kết nối lại...");
            try {
                if (connectWithRetry()) {
                    out.println(message);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                        if (!in.ready()) break;
                    }
                    return sb.toString();
                }
            } catch (Exception retryEx) {
                System.err.println("Retry thất bại: " + retryEx.getMessage());
            }
            return "ERROR|Mất kết nối server!";
        }
    }

    public String receive() {
        try {
            if (!isConnected()) return null;
            return in.readLine();
        } catch (Exception e) {
            System.err.println("Lỗi nhận: " + e.getMessage());
            socket = null;
            return null;
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public void disconnect() {
        synchronized (ServerConnection.class) {
            try {
                if (socket != null) socket.close();
                socket = null;
                instance = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnectDirect() {
        try {
            if (socket != null) socket.close();
            socket = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}