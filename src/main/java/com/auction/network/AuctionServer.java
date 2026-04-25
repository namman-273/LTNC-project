package com.auction.network;

import com.auction.service.AuctionService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private final int port;
    private final AuctionService auctionService;

    public AuctionServer(int port, AuctionService auctionService) {
        this.port = port;
        this.auctionService = auctionService;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SERVER: Đang chạy trên cổng " + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("SERVER: Có khách hàng mới kết nối!");

                ClientHandler handler = new ClientHandler(clientSocket, auctionService);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("SERVER ERROR: " + e.getMessage());
        }
    }
}
