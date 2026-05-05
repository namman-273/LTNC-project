package com.auction.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

  private static final String HOST = "localhost";
  private static final int PORT = 9999;
  private String host;
  private int port;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  private static ServerConnection instance;

  private ServerConnection() {
  }

  public static ServerConnection getInstance() {
    if (instance == null) {
      instance = new ServerConnection();
    }
    return instance;
  }

  public boolean connect() {
    try {
      // Nếu đã kết nối rồi thì không kết nối lại
      if (socket != null && !socket.isClosed() && socket.isConnected()) {
        return true;
      }
      socket = new Socket(HOST, PORT);
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

  public String sendAndReceive(String message) {
    try {
      if (!isConnected()) {
        return "ERROR|Mất kết nối server!";
      }
      out.println(message);
      return in.readLine();
    } catch (Exception e) {
      System.err.println("Lỗi gửi/nhận: " + e.getMessage());
      socket = null;
      return "ERROR|Mất kết nối server!";
    }
  }

  public String receive() {
    try {
      if (!isConnected()) {
        return null;
      }
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
    try {
      if (socket != null) {
        socket.close();
      }
      socket = null;
      instance = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ServerConnection(String host, int port) {
    this.host = host;
    this.port = port;
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
}
