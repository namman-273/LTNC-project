package com.auction.network;

import com.auction.model.User;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.service.AuctionService;
import com.auction.model.Observer;
import com.auction.service.UserManager; // Import UserManager Singleton

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

    private User currentUser;

    public ClientHandler(final Socket socket, final AuctionService service) {
        this.socket = socket;
        this.auctionService = service;
    }

    @Override
    public final void run() {
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
                    case "GET_HISTORY": // Lệnh: GET_HISTORY|auctionId
                        handleGetHistory(parts);
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

    private void handleRegister(final String[] parts) {
        // REGISTER|username|password|role
        boolean success = UserManager.getInstance().register(parts[1], parts[2], parts[3]);
        if (success) {
            out.println("REGISTER_SUCCESS|Đăng ký thành công.");
        } else {
            out.println("REGISTER_FAILED|Tên người dùng đã tồn tại.");
        }
    }

    private void handleLogin(final String[] parts) {
        // LOGIN|username|password
        String username = parts[1];
        String password = parts[2];

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

    private void handleCreateAuction(final String[] parts) {
    // CREATE_AUCTION|type|name|startingPrice|durationMinutes
    if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
        out.println("ERROR|Quyền hạn không đủ.");
        return;
    }
    try {
        String type = parts[1];
        String name = parts[2];
        double price = Double.parseDouble(parts[3]);
        long duration = Long.parseLong(parts[4]);

        // Giả sử auctionService có hàm create mới
        auctionService.createNewAuction(type, name, price, duration);
        out.println("SUCCESS|Sản phẩm " + name + " đã được đăng sàn.");
    } catch (Exception e) {
        out.println("ERROR|Dữ liệu tạo sản phẩm không hợp lệ.");
    }
}

    private void handleEndAuction(final String[] parts) {
        // Kiểm tra quyền: Chỉ Admin mới được đóng phiên thủ công
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            out.println("ERROR|Chỉ Admin mới có quyền đóng phiên.");
            return;
        }
        auctionService.endAuction(parts[1]); // parts[1] là auctionId
        out.println("END_SUCCESS|Đã đóng phiên " + parts[1]);
    }

    private void handleBid(final String[] parts) {
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
            auction.processNewBid((Bidder) currentUser, amount);
            out.println("BID_SUCCESS|" + auctionId + "|" + amount);
        } catch (Exception e) {
            out.println("ERROR|" + e.getMessage());
        }
    }

    private void handleGetHistory(final String[] parts) {
        String auctionId = parts[1];
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction != null) {
            StringBuilder historyData = new StringBuilder("HISTORY_RES|" + auctionId);
            // Duyệt lịch sử để gửi về cho FE vẽ LineChart
            for (BidTransaction tx : auction.getBidHistory()) {
                historyData.append("|").append(tx.getBidder().getUsername())
                        .append(",").append(tx.getAmount());
            }
            out.println(historyData.toString());
        } else {
            out.println("ERROR|Không tìm thấy phiên đấu giá.");
        }
    }

    public final void sendMessage(final String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public final void update(final String msg) {
        this.sendMessage(msg);
    }

    private void cleanUp() {
        try {
            if (auctionService != null) {
                for (Auction a : auctionService.getAllAuctions()) {
                    a.removeObserver(this);
                }
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
