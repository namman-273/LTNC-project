package com.auction.network.server;

import com.auction.controller.network.ClientHandler;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.usermanger.UserManager;
import com.auction.util.core.datamanager.DataManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server Auction
 * - Lắng nghe kết nối từ Client
 * - Tối ưu: In IP máy server để Client kết nối.
 */
public class AuctionServer {
  private final int port;

  // Trường này để điều khiển việc dừng
  private static volatile boolean running = true;
  private ServerSocket serverSocket;

  public AuctionServer(int port) {
    this.port = port;
  }

  /**
   * Server chạy.
   */
  public void start() {
    // Đăng ký Shutdown Hook: Tự động chạy khi nhấn Stop/Ctrl+C
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\n[SYSTEM] Đang tiến hành đóng Server...");
      running = false;
      try {
        if (serverSocket != null && !serverSocket.isClosed()) {
          serverSocket.close(); // Giải phóng block ở accept()
        }
        // Gọi shutdown của Service để lưu file .dat
        AuctionService.getInstance().shutdown();

      } catch (IOException e) {
        System.err.println("Lỗi khi đóng socket: " + e.getMessage());
      }
    }));

    try {
      serverSocket = new ServerSocket(port);

      System.out.println("\n╔════════════════════════════════════════════╗");
      System.out.println("║     🟢 SERVER AUCTION ĐANG CHẠY          ║");
      System.out.println("╠════════════════════════════════════════════╣");
      System.out.println("║  Port: " + port);

      try {
        // Lấy IP máy server
        java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
        String ipAddress = localHost.getHostAddress();
        System.out.println("║  IP máy này: " + ipAddress);
        System.out.println("║");
        System.out.println("║  📝 Client hãy sửa server.properties:");
        System.out.println("║     server.host=" + ipAddress);
        System.out.println("║     server.port=" + port);
      } catch (Exception e) {
        System.out.println("║  (Không lấy được IP)");
      }

      System.out.println("╚════════════════════════════════════════════╝\n");
      System.out.println("SERVER: Đang chạy trên cổng " + port);

      while (running) {
        try {
          Socket clientSocket = serverSocket.accept();
          System.out.println("Client kết nối từ: "
              + clientSocket.getInetAddress().getHostAddress());
          ClientHandler handler = new ClientHandler(clientSocket);
          new Thread(handler).start();
        } catch (SocketException e) {
          // Khi serverSocket bị close bởi Hook, accept() sẽ ném ra Exception này
          if (!running) {
            System.out.println("[SYSTEM] Server đã dừng nhận kết nối.");
          } else {
            throw e;
          }
        }
      }
    } catch (IOException e) {
      if (running) {
        System.err.println("SERVER ERROR: " + e.getMessage());
      }
    }
  }

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    // Khởi tạo các Manager
    DataManager.getInstance().loadData();

    // Kiểm tra nếu chưa có admin thì mới tạo
    UserManager.getInstance().initDefaultData();

    AuctionServer server = new AuctionServer(9999);
    System.out.println("Khởi động server tại port 9999...");

    server.start();
  }
}