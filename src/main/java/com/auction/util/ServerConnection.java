package com.auction.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private static ServerConnection instance;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Kết nối server thành công!");
            return true;
        } catch (Exception e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
            return false;
        }
    }

    public String sendAndReceive(String message) {
        try {
            out.println(message);
            return in.readLine();
        } catch (Exception e) {
            System.err.println("Lỗi gửi/nhận: " + e.getMessage());
            return null;
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}