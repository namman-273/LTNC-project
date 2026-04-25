package com.auction.network;

import com.auction.model.*;
import com.auction.service.AuctionService;
import com.auction.service.UserManager; // Import UserManager Singleton

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable, Observer {
    private Socket socket;
    private AuctionService auctionService;
    private PrintWriter out;
    private BufferedReader in;
    private User currentUser; // Thay Bidder bằng User để đa dạng vai trò (Admin/Seller/Bidder)

    public ClientHandler(Socket socket, AuctionService service) {
        this.socket = socket;
        this.auctionService = service;
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

                switch (cmd) {
                    case "REGISTER": // Lệnh mới: REGISTER|username|password|role
                        handleRegister(parts);
                        break;
                    case "LOGIN": // Sửa lại: LOGIN|username|password
                        handleLogin(parts);
                        break;
                    case "LIST_AUCTIONS": // Lệnh mới: Xem danh sách phiên
                        handleListAuctions();
                        break;
                    case "BID":
                        handleBid(parts);
                        break;
                    case "CREATE_AUCTION": // Lệnh mới: Tạo phiên (Chỉ Seller/Admin)
                        handleCreateAuction(parts);
                        break;
                    case "END_AUCTION": // Lệnh mới: Đóng phiên (Chỉ Admin)
                        handleEndAuction(parts);
                        break;
                    default:
                        out.println("ERROR|Lệnh không hợp lệ");
                }
            }
        } catch (IOException e) {
            System.out.println("Một client đã ngắt kết nối.");
        } finally {
            cleanUp();
        }
    }

    private void handleRegister(String[] parts) {
        // REGISTER|username|password|role
        boolean success = UserManager.getInstance().register(parts[1], parts[2], parts[3]);
        if (success) {
            out.println("REGISTER_SUCCESS|Đăng ký thành công.");
        } else {
            out.println("REGISTER_FAILED|Tên người dùng đã tồn tại.");
        }
    }

    private void handleLogin(String[] parts) {
        // LOGIN|username|password
        String username = parts[1];
        String password = parts[2];

        System.out.println("DEBUG username='" + username + "' password='" + password + "'");
        System.out.println("DEBUG user found: " + UserManager.getInstance().findUserById(username));
        
        // Kiểm tra đăng nhập thực sự từ UserManager
        User user = UserManager.getInstance().login(username, password);

        if (user != null) {
            this.currentUser = user;
            // Tự động cho người dùng theo dõi các phiên đấu giá
            for (Auction auction : auctionService.getAllAuctions()) {
                auction.addObserver(this);
            }
            out.println("LOGIN_SUCCESS|" + user.getRole() + "|Chào " + user.getUsername());
        } else {
            out.println("LOGIN_FAILED|Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    private void handleListAuctions() {
        // Trả về danh sách thô (Trong thực tế nên dùng JSON hoặc toString chuẩn)
        out.println("LIST_AUCTIONS_SUCCESS|" + auctionService.getAllAuctions().toString());
    }

    private void handleCreateAuction(String[] parts) {
        // Kiểm tra quyền: Chỉ Seller hoặc Admin mới được tạo
        if (currentUser == null || "BIDDER".equals(currentUser.getRole())) {
            out.println("ERROR|Bạn không có quyền tạo phiên đấu giá.");
            return;
        }
        // Logic tạo Item bằng Factory Method ở Bước 1 và thêm vào AuctionService
        // CREATE_AUCTION|type|id|name|price
        out.println("SUCCESS|Yêu cầu tạo phiên đã được ghi nhận.");
    }

    private void handleEndAuction(String[] parts) {
        // Kiểm tra quyền: Chỉ Admin mới được đóng phiên thủ công
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            out.println("ERROR|Chỉ Admin mới có quyền đóng phiên.");
            return;
        }
        auctionService.endAuction(parts[1]); // parts[1] là auctionId
        out.println("END_SUCCESS|Đã đóng phiên " + parts[1]);
    }

    private void handleBid(String[] parts) {
        if (this.currentUser == null) {
            out.println("ERROR|Bạn phải đăng nhập trước khi đấu giá!");
            return;
        }
        // Giữ nguyên logic bid cũ nhưng đổi currentBidder thành currentUser
        try {
            String auctionId = parts[1];
            double amount = Double.parseDouble(parts[2]);
            Auction auction = auctionService.getAuctionById(auctionId);
            
            // Ép kiểu currentUser về Bidder để khớp với method cũ hoặc sửa method nhận User
            auction.processNewBid((Bidder)currentUser, amount); 
            out.println("BID_SUCCESS|" + auctionId + "|" + amount);
        } catch (Exception e) {
            out.println("ERROR|" + e.getMessage());
        }
    }

    public void sendMessage(String msg) { if (out != null) out.println(msg); }
    public void update(String msg) { this.sendMessage(msg); }

    private void cleanUp() {
        try {
            if (auctionService != null) {
                for (Auction a : auctionService.getAllAuctions()) { a.removeObserver(this); }
            }
            if (socket != null) socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
