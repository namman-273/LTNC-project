package com.auction.network;

import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Observer;
import com.auction.service.AuctionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable, Observer {
    private Socket socket;
    private AuctionService auctionService;
    private PrintWriter out;
    private BufferedReader in;
    private Bidder currentBidder;

    public ClientHandler(Socket socket,AuctionService service){
        this.socket=socket;this.auctionService=service;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.split("\\|");
                String cmd = parts[0];

                if ("LOGIN".equals(cmd)) {
                    handleLogin(parts);
                } else if ("BID".equals(cmd)) {
                    handleBid(parts);
                }
            }
        } catch (IOException e) {
            System.out.println("Một client đã ngắt kết nối.");
        } finally {
            cleanUp();
        }
    }
    private void handleLogin(String[] parts) {
        String username = parts[1];
        String id = "B_" + System.currentTimeMillis();
        this.currentBidder = new Bidder(id, username);

        // Cho người này theo dõi TẤT CẢ các phiên đấu giá đang có
        for (Auction auction : auctionService.getAllAuctions()) {
            auction.addObserver(this);
        }

        out.println("SUCCESS|Chào " + username + ". Bạn đã được đăng ký nhận tin Real-time.");
    }
    private void handleBid(String[] parts) {
        if (this.currentBidder == null) {
            out.println("ERROR|Bạn phải đăng nhập trước khi đấu giá!");
            return;
        }
        try {
            String auctionId = parts[1];
            double amount = Double.parseDouble(parts[2]);

            Auction auction = auctionService.getAuctionById(auctionId);
            auction.processNewBid(currentBidder, amount);
            // Trả về cho chính họ, phần "phát loa" (Broadcast) sẽ xử lý ở bước sau
            out.println("BID_SUCCESS|" + auctionId + "|" + amount);
        } catch (Exception e) {
            out.println("ERROR|" + e.getMessage());
        }
    }
    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    public void update(String msg) {
        this.sendMessage(msg);
    }

    private void cleanUp() {
        try {
            // Gỡ client này ra khỏi tất cả các phiên đấu giá
            if (auctionService != null) {
                for (Auction a : auctionService.getAllAuctions()) {
                    a.removeObserver(this);
                }
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
