package com.auction.network;

import com.auction.service.AuctionService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import com.auction.service.UserManager;

public class AuctionServer {
    private final int port;

    // trường này để điều khiển việc dừng
    private static volatile boolean running = true;
    private ServerSocket serverSocket;

    public AuctionServer(int port) {
        this.port = port;
    }

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
            System.out.println("SERVER: Đang chạy trên cổng " + port);

            // Thay while(true) bằng running
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("SERVER: Có khách hàng mới kết nối!");
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

    public static void main(String[] args) {
        // Khởi tạo các Manager
        UserManager.getInstance();
        AuctionService auctionService = AuctionService.getInstance();

        AuctionServer server = new AuctionServer(9999);
        System.out.println("Khởi động server tại port 9999...");
        server.start();
    }
}